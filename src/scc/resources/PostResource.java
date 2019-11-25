package scc.resources;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.DocumentClientException;
import com.microsoft.azure.cosmosdb.FeedResponse;

import scc.models.Like;
import scc.models.Post;
import scc.models.PostWithReplies;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.Config;
import scc.utils.GSON;
import scc.utils.MyBase64;

public class PostResource {

	static Logger logger = LoggerFactory.getLogger(PostResource.class);

	public static boolean exists(String post_id) {
		try {
			return get(post_id) != null;
		} catch (WebApplicationException e) {
			return false;
		}
	}

	public static boolean existsLike(String like_id) {
		return CosmosClient.getById(Config.LIKES_CONTAINER, like_id) != null;
	}

	public static String create(Post post) {
		try {
			post.correctPost();
			
			if (!post.validPost())
				throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(post).build());

			if (!UserResource.exists(post.getAuthor()))
				throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Username %s does not exist", post.getAuthor())).build());

			if (!CommunityResource.exists(post.getCommunity()))
				throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Community %s does not exist", post.getCommunity())).build());

			if(post.getImage() != null && !ImageResource.exists(post.getImage() ))
				throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Image %s does not exist", post.getImage())).build());

			String post_id = null;
			if (post.isReply()) {
				Post parent = get(post.getParent());

				if(parent == null)
					throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Parent post %s does not exist", post.getParent())).build());

				if(!parent.getCommunity().equals(post.getCommunity()))
					throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(String.format("Community %s is different from parent's community %s", post.getCommunity(), parent.getCommunity())).build());

				post_id = CosmosClient.insert(Config.POSTS_CONTAINER, post);

				// Update cache
				Redis.LRUHyperLogUpdate(Config.TOTAL_REPLIES, post.getParent(), post_id, false);
				Redis.LRUHyperLogUpdate(Config.DAYLY_REPLIES, post.getParent(), post_id, false);

				List<Entry<String, Entry<String, String>>> pages = Redis.LRUPairGetAll(Config.TOP_REPLIES, post.getParent() + ":*");
				List<String> toDelete = pages.stream().filter(p -> p.getValue().getValue() == null).map(p -> p.getKey()).collect(Collectors.toList());
				Redis.del(toDelete);
			} else {
				post_id = CosmosClient.insert(Config.POSTS_CONTAINER, post);
			}

			return post_id;
		} catch (DocumentClientException e) {
			if (e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity("Post with that ID already exists").build());
			else
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}

	public static Post get(String post_id) {
		String post_json = Redis.LRUDictionaryGet(Config.TOP_POSTS, post_id);
		if (post_json == null) {
			Post post = CosmosClient.getByIdUnparse(Config.POSTS_CONTAINER, post_id, Post.class);

			if (post == null)
				throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Post %s does not exist.", post_id)).build());

			post_json = GSON.toJson(post);

			Redis.LRUDictionaryPut(Config.TOP_POSTS, Integer.parseInt(Config.getRedisProperty(Config.TOP_POSTS_LIMIT)), post_id, post_json);

			return post;
		}

		return GSON.fromJson(post_json, Post.class);
	}

	public static String likePost(String post_id, String username) {

		if (!UserResource.exists(username))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Username %s does not exist", username)).build());

		if (!PostResource.exists(post_id))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Post %s does not exist", post_id)).build());

		try {
			Like like = new Like(post_id, username);
			String like_id = CosmosClient.insert(Config.LIKES_CONTAINER, like);

			// If in cache, update
			Redis.LRUHyperLogUpdate(Config.TOTAL_LIKES, post_id, like_id, false);
			Redis.LRUHyperLogUpdate(Config.DAYLY_LIKES, post_id, like_id, false);

			return like_id;
		} catch (DocumentClientException e) {
			if (e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity(String.format("User %s have already liked post %s", username, post_id)).build());
			else
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}

	public static void dislikePost(String post_id, String username) {

		String like_id = Like.buildId(post_id, username);

		if (!PostResource.existsLike(like_id))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("User %s have not yet liked that post %s", username, post_id)).build());

		boolean deleted = CosmosClient.delete(Config.LIKES_CONTAINER, like_id, like_id) > 0;

		// If in cache, set dirty bit to true
		Redis.LRUSetDirtyBit(Config.TOTAL_LIKES, post_id, true);
		Redis.LRUSetDirtyBit(Config.DAYLY_LIKES, post_id, true);

		if (!deleted)
			throw new WebApplicationException(Response.status(Status.CONFLICT).entity(String.format("User %s have already disliked post %s", username, post_id)).build());
	}

	public static long getTotalLikes(String post_id) {

		logger.info("getTotalLikes " + post_id);

		Long total_likes = Redis.LRUHyperLogGet(Config.TOTAL_LIKES, post_id);
		if (total_likes == null) {
			if (Redis.ACTIVE) {
				String query = "SELECT * FROM %s l WHERE l.post_id='" + post_id + "'";
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(Config.LIKES_CONTAINER, query);
				if (it.hasNext()) {
					List<Like> likes = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Like.class)).collect(Collectors.toList());
					Redis.LRUHyperLogPut(Config.TOTAL_LIKES, Integer.parseInt(Config.getRedisProperty(Config.TOTAL_LIKES_LIMIT)), post_id, likes.stream().map(l -> l.getId()).collect(Collectors.toList()));
					total_likes = (long) likes.size();
				} else
					total_likes = 0L;
				while (it.hasNext()) {
					List<String> likes = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Like.class).getId()).collect(Collectors.toList());
					Redis.LRUHyperLogUpdate(Config.TOTAL_LIKES, post_id, likes, false);

					total_likes += likes.size();
				}
			} else {
				String query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='" + post_id + "'";
				List<String> likes = CosmosClient.query(Config.LIKES_CONTAINER, query);
				if (!likes.isEmpty()) {
					JsonElement root = JsonParser.parseString(likes.get(0));
					total_likes = root.getAsJsonObject().get("Likes").getAsLong();
				} else
					total_likes = 0L;
			}
		}
		return total_likes.longValue();
	}

	public static Entry<String, List<PostWithReplies>> getReplies(String post_id, String continuation_token, int page_size) {

		boolean first = true;
		List<PostWithReplies> replies = null;
		Entry<String, List<PostWithReplies>> entry = null;
		String next_continuation_token = null;

		Entry<String, String> pair = Redis.LRUPairGet(Config.TOP_REPLIES, post_id + ":" + continuation_token);
		if (pair == null) {
			String unparsed_continuation_token = MyBase64.decodeString(continuation_token);
			String query_replies = "SELECT * FROM %s p WHERE p.parent='" + post_id + "' ORDER BY p._ts ASC";
			entry = CosmosClient.queryAndUnparsePaginated(Config.POSTS_CONTAINER, query_replies, unparsed_continuation_token, page_size, PostWithReplies.class);
			replies = entry.getValue();
			next_continuation_token = MyBase64.encode(entry.getKey());

			Redis.LRUPairPut(Config.TOP_REPLIES, Integer.parseInt(Config.getRedisProperty(Config.TOP_REPLIES_LIMIT)), post_id + ":" + continuation_token, GSON.toJson(replies), next_continuation_token);

			List<String> replies_ids = replies.stream().map(r -> r.getId()).collect(Collectors.toList());

			if (first) {
				first = false;
				Redis.LRUHyperLogPut(Config.TOTAL_REPLIES, Integer.parseInt(Config.getRedisProperty(Config.TOTAL_REPLIES_LIMIT)), post_id, replies_ids);
			} else {
				Redis.LRUHyperLogUpdate(Config.TOTAL_REPLIES, post_id, replies_ids, false);
			}
		} else {
			replies = GSON.fromJson(pair.getKey(), new TypeToken<List<PostWithReplies>>() {
			}.getType());
			next_continuation_token = pair.getValue();
		}

		return new AbstractMap.SimpleEntry<String, List<PostWithReplies>>(next_continuation_token, replies);
	}

	public static long getTotalReplies(String post_id, PostWithReplies post, int page_size) {
		Long total_replies = Redis.LRUHyperLogGet(Config.TOTAL_REPLIES, post_id);
		if (total_replies == null) {
			if (Redis.ACTIVE) {
				String continuation_token = null;
				boolean first = true;
				do {
					total_replies = 0L;

					Entry<String, List<PostWithReplies>> entry = getReplies(post_id, continuation_token, page_size);
					List<PostWithReplies> replies = entry.getValue();
					continuation_token = entry.getKey();

					if (first && post != null) {
						first = false;
						post.setReplies(replies);
						post.setContinuationToken(continuation_token);
					}

					total_replies += replies.size();
				} while (continuation_token != null);
			} else {
				String query = "SELECT COUNT(p) as Replies FROM %s p WHERE p.parent='" + post_id + "'";
				List<String> likes = CosmosClient.query(Config.POSTS_CONTAINER, query);
				if (!likes.isEmpty()) {
					JsonElement root = JsonParser.parseString(likes.get(0));
					total_replies = root.getAsJsonObject().get("Replies").getAsLong();
				} else
					total_replies = 0L;
			}
		}
		return total_replies;
	}

	public static long getRecentLikes(String post_id, long time_s) {
		Long n_likes = Redis.LRUHyperLogGet(Config.DAYLY_LIKES, post_id);
		if (n_likes == null) {
			if (Redis.ACTIVE) {
				String query = "SELECT * FROM %s l WHERE l.post_id='" + post_id + "' AND l._ts>=" + time_s;
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(Config.LIKES_CONTAINER, query);
				if (it.hasNext()) {
					List<Like> likes = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Like.class)).collect(Collectors.toList());
					Redis.LRUHyperLogPut(Config.DAYLY_LIKES, Integer.parseInt(Config.getRedisProperty(Config.DAYLY_LIKES_LIMIT)), post_id, likes.stream().map(l -> l.getId()).collect(Collectors.toList()));
					n_likes = (long) likes.size();
				} else
					n_likes = 0L;

				while (it.hasNext()) {
					List<String> likes = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Like.class).getId()).collect(Collectors.toList());
					Redis.LRUHyperLogUpdate(Config.DAYLY_LIKES, post_id, likes, false);

					n_likes += likes.size();
				}
			} else {
				String query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='" + post_id + "' AND l._ts>=" + time_s;
				List<String> likes = CosmosClient.query(Config.LIKES_CONTAINER, query);
				if (!likes.isEmpty()) {
					JsonElement root = JsonParser.parseString(likes.get(0));
					n_likes = root.getAsJsonObject().get("Likes").getAsLong();
				} else
					n_likes = 0L;
			}
		}
		return n_likes;
	}

	public static long getRecentReplies(String post_id, long time_s) {
		Long n_replies = Redis.LRUHyperLogGet(Config.DAYLY_REPLIES, post_id);
		if (n_replies == null) {
			if (Redis.ACTIVE) {
				String query = "SELECT * FROM %s p WHERE p.parent='" + post_id + "' AND p._ts>=" + time_s;
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(Config.POSTS_CONTAINER, query);
				if (it.hasNext()) {
					List<Post> replies = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Post.class)).collect(Collectors.toList());
					Redis.LRUHyperLogPut(Config.DAYLY_REPLIES, Integer.parseInt(Config.getRedisProperty(Config.DAYLY_REPLIES_LIMIT)), post_id, replies.stream().map(r -> r.getId()).collect(Collectors.toList()));
					n_replies = (long) replies.size();
				} else
					n_replies = 0L;

				while (it.hasNext()) {
					List<String> posts = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Post.class).getId()).collect(Collectors.toList());
					Redis.LRUHyperLogUpdate(Config.DAYLY_REPLIES, post_id, posts, false);

					n_replies += posts.size();
				}
			} else {
				String query = "SELECT COUNT(p) as Replies FROM %s p WHERE p.parent='" + post_id + "' AND p._ts>=" + time_s;
				List<String> replies = CosmosClient.query(Config.POSTS_CONTAINER, query);
				if (!replies.isEmpty()) {
					JsonElement root = JsonParser.parseString(replies.get(0));
					n_replies = root.getAsJsonObject().get("Replies").getAsLong();
				} else
					n_replies = 0L;
			}
		}
		return n_replies;
	}
}

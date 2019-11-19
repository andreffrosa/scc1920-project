package scc.controllers;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedResponse;
import scc.models.Like;
import scc.models.Post;
import scc.models.PostWithReplies;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.GSON;
import scc.utils.MyBase64;

@Path(PagesResource.PATH)
public class PagesResource {

	public static final String PATH = "/page";
	private static final int DEFAULT_INITIAL_PAGE_SIZE = 10;
	private static final int DEFAULT_LEVEL = 3;
	private static final int DEFAULT_PAGE_SIZE = 3;
	//private static final int MAX_SIZE_ALLOWED = 2;

	@GET
	@Path("/thread/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public PostWithReplies getThread(@PathParam("id") String id, @DefaultValue(""+DEFAULT_LEVEL) @QueryParam("d") int depth, @DefaultValue(""+DEFAULT_PAGE_SIZE) @QueryParam("p") int pageSize, @QueryParam("t") String continuation_token) {

		String post_json = PostResource.getPost(id);
		PostWithReplies post = GSON.fromJson(post_json, PostWithReplies.class);

		Queue<PostWithReplies> queue = new LinkedList<>();
		queue.add(post);
		int current_level = 0, amount_posts_current_level = 1;
		while(!queue.isEmpty()) { 
			PostWithReplies current_post = queue.poll();
			amount_posts_current_level--;

			String post_id = current_post.getId();

			Entry<String, List<PostWithReplies>> entry = getReplies(post_id, continuation_token);
			List<PostWithReplies> replies = entry.getValue();
			String next_continuation_token = entry.getKey();

			current_post.setReplies(replies);
			current_post.setContinuationToken(next_continuation_token);

			// Total Likes
			long total_likes = getTotalLikes(post_id);
			current_post.setLikes(total_likes);

			if(current_level < depth) {
				queue.addAll(replies);
			}

			if(amount_posts_current_level == 0) {
				current_level++;
				amount_posts_current_level = queue.size();
			}
		}

		return post;
	}

	@GET
	@Path("/initial")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PostWithReplies> getInitialPage(@DefaultValue(""+DEFAULT_INITIAL_PAGE_SIZE) @QueryParam("ps") int n_posts/*, @DefaultValue(""+MAX_SIZE_ALLOWED) @QueryParam("m") int max_size*/) {

		//TODO: verificar se o n_posts é menor que o valor que está a ser calculado pela função.

		try {
			List<String> fromCache = Redis.getPaginatedList(Redis.INITIAL_PAGE, n_posts);
			if(fromCache!= null && !fromCache.isEmpty()){
				return fromCache.stream().map(d -> GSON.fromJson(d , PostWithReplies.class))
						.collect(Collectors.toList()); // TODO: Porque não guardar apenas o Json da lista logo? -> ara poder obter apenas parte da lista
			} else {
				Comparator<Entry<Integer, PostWithReplies>> comp = (x, y) -> x.getKey().compareTo(y.getKey());
				Queue<Entry<Integer, PostWithReplies>> queue = new PriorityQueue<Entry<Integer, PostWithReplies>>(n_posts, comp);

				String query = "SELECT * FROM %s p WHERE p.parent=null";
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(PostResource.POSTS_CONTAINER, query);
				while( it.hasNext() ) {
					Iterable<PostWithReplies> postsWithReplies = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), PostWithReplies.class)).collect(Collectors.toList());
					for (PostWithReplies p : postsWithReplies) {
						int score = getScore(p);
						if (queue.size() < n_posts) {
							queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(score, p));
						} else {
							Entry<Integer, PostWithReplies> e = queue.peek();
							if (queue.size() >= n_posts) {
								if (e.getKey() < score) {
									queue.poll();
									queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(score, p));
								} else if (e.getKey() == score) {
									if (Math.random() <= 0.5) { // Replace with 50% probability
										queue.poll();
										queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(score, p));
									}
								}
							}
						}
					}
				}

				List<PostWithReplies> list = queue.stream().map(e -> e.getValue()).collect(Collectors.toList());

				Redis.del(Redis.INITIAL_PAGE);
				Redis.putInList(Redis.INITIAL_PAGE, list.stream().map(e -> GSON.toJson(e)).toArray(String[]::new));

				return list;
			}
		} catch(Exception e) {
			throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}

	private static long getTotalLikes(String post_id) {
		Long total_likes = Redis.LRUHyperLogGet(Redis.TOTAL_LIKES, post_id);
		if(total_likes == null) {
			if(Redis.ACTIVE) {
				String query = "SELECT * FROM %s l WHERE l.post_id='" + post_id + "'";
				List<Like> likes = CosmosClient.queryAndUnparse(PostResource.LIKES_CONTAINER, query, Like.class);
				total_likes = (long) likes.size();
				Redis.LRUHyperLogPut(Redis.TOTAL_LIKES, Redis.TOTAL_LIKES_LIMIT, post_id, likes.stream().map(l -> GSON.toJson(l)).collect(Collectors.toList()));
			} else {
				String query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='" + post_id + "'";
				List<String> likes = CosmosClient.query(PostResource.LIKES_CONTAINER, query);
				if (!likes.isEmpty()) {
					JsonElement root = JsonParser.parseString(likes.get(0));
					total_likes = root.getAsJsonObject().get("Likes").getAsLong();
				} else
					total_likes = 0L;
			}
		}
		return total_likes.longValue();
	}

	private static Entry<String,List<PostWithReplies>> getReplies(String post_id, String continuation_token) {

		boolean first = true;
		List<PostWithReplies> replies = null;
		Entry<String,List<PostWithReplies>> entry = null;
		String next_continuation_token = null;

		Entry<String,String> pair = Redis.LRUPairGet(Redis.TOP_REPLIES, post_id + ":" + continuation_token);
		if(pair == null) {
			String unparsed_continuation_token = continuation_token == null ? null : MyBase64.decodeString(continuation_token);
			String query_replies = "SELECT * FROM %s p WHERE p.parent='" + post_id +"'";
			entry = CosmosClient.queryAndUnparsePaginated(PostResource.POSTS_CONTAINER, query_replies, unparsed_continuation_token, DEFAULT_PAGE_SIZE, PostWithReplies.class);
			next_continuation_token = MyBase64.encode(entry.getKey());
			replies = entry.getValue();

			Redis.LRUPairPut(Redis.TOP_REPLIES, Redis.TOP_REPLIES_LIMIT, post_id + ":" + continuation_token, GSON.toJson(replies), next_continuation_token);

			List<String> replies_json = replies.stream().map(r -> GSON.toJson(r)).collect(Collectors.toList());

			if(first) {
				first = false;
				Redis.LRUHyperLogPut(Redis.TOTAL_REPLIES, Redis.TOTAL_REPLIES_LIMIT, post_id, replies_json);
			} else {
				Redis.LRUHyperLogUpdate(Redis.TOTAL_REPLIES, post_id, replies_json, false);
			}
		} else {
			replies = GSON.fromJson(pair.getValue(), new TypeToken<List<PostWithReplies>>(){}.getType());
			next_continuation_token = pair.getKey();
			entry = new AbstractMap.SimpleEntry<String, List<PostWithReplies>>(next_continuation_token, replies);
		}

		return entry;
	}

	private static int getFreshness(PostWithReplies p) {
		double days = ((System.currentTimeMillis() / 1000) - p.getCreationTime() + 1) / ((double)( 24 * 60 * 60 ));

		if(days < 0.3)
			days = 0.3;

		int freshness = (int)Math.round(100.0/days);
		return freshness;
	}

	private static int getPopularity(PostWithReplies p) {

		// Total Replies
		Long total_replies = Redis.LRUHyperLogGet(Redis.TOTAL_REPLIES, p.getId());
		if(total_replies == null) {
			if(Redis.ACTIVE) {
				String continuation_token = null;
				boolean first = true;
				do {
					total_replies = 0L;

					Entry<String, List<PostWithReplies>> entry = getReplies(p.getId(), continuation_token);
					List<PostWithReplies> replies = entry.getValue();
					continuation_token = entry.getKey();

					if(first) {
						first = false;
						p.setReplies(replies);
						p.setContinuationToken(continuation_token);
					}

					total_replies += replies.size();
				} while( continuation_token != null );
			} else {
				String query = "SELECT COUNT(p) as Replies FROM %s p WHERE p.parent='" + p.getId() + "'";
				List<String> likes = CosmosClient.query(PostResource.POSTS_CONTAINER, query);
				if (!likes.isEmpty()) {
					JsonElement root = JsonParser.parseString(likes.get(0));
					total_replies = root.getAsJsonObject().get("Replies").getAsLong();
				} else
					total_replies = 0L;
			}
		}

		p.setReplies(null);

		// Total Likes
		long total_likes = getTotalLikes(p.getId());
		p.setLikes(total_likes);

		int n_likes = total_likes == 0 ? 0 :(int) Math.log10(total_likes);
		int n_replies = total_replies == 0 ? 0 :(int) Math.log10(total_replies);
		int a = (int) Math.round(0.8 * n_likes + 0.2 * n_replies);
		int b = (int) Math.round(0.2 * n_likes + 0.8 * n_replies);
		int popularity = Math.max(a, b);
		return popularity;
	}

	private static int getHotness(PostWithReplies p) {
		long time = (System.currentTimeMillis() / 1000) - (24 * 60 * 60);

		// Replies in last 24h
		Long n_replies = Redis.LRUHyperLogGet(Redis.DAYLY_REPLIES, p.getId());
		if(n_replies == null) {
			if(Redis.ACTIVE) {
				String query = "SELECT * FROM %s p WHERE p.parent='" + p.getId() + "' AND p._ts>=" + time;	
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(PostResource.POSTS_CONTAINER, query);
				if(it.hasNext() ) {
					List<Post> replies = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Post.class)).collect(Collectors.toList());
					Redis.LRUHyperLogPut(Redis.DAYLY_REPLIES, Redis.DAYLY_REPLIES_LIMIT, p.getId(), replies.stream().map(r -> r.getId()).collect(Collectors.toList()));
					n_replies = (long) replies.size();
				} else
					n_replies = 0L;

				while( it.hasNext() ) {
					List<String> posts = it.next().getResults().stream().map((Document d) -> GSON.toJson(GSON.fromJson(d.toJson(), Post.class))).collect(Collectors.toList());
					Redis.LRUHyperLogUpdate(Redis.DAYLY_REPLIES, p.getId(), posts, false);

					n_replies += posts.size();
				}
			} else {
				String query = "SELECT COUNT(p) as Replies FROM %s p WHERE p.parent='" + p.getId() + "' AND p._ts>=" + time;
				List<String> replies = CosmosClient.query(PostResource.POSTS_CONTAINER, query);
				if (!replies.isEmpty()) {
					JsonElement root = JsonParser.parseString(replies.get(0));
					n_replies = root.getAsJsonObject().get("Replies").getAsLong();
				} else
					n_replies = 0L;
			}
		}

		// Likes in last 24h
		Long n_likes = Redis.LRUHyperLogGet(Redis.DAYLY_LIKES, p.getId());
		if(n_likes == null) {
			if(Redis.ACTIVE) {
				String query = "SELECT * FROM %s l WHERE l.post_id='" + p.getId() + "' AND l._ts>=" + time;
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(PostResource.LIKES_CONTAINER, query);
				if(it.hasNext() ) {
					List<Like> likes = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Like.class)).collect(Collectors.toList());
					Redis.LRUHyperLogPut(Redis.DAYLY_LIKES, Redis.DAYLY_LIKES_LIMIT, p.getId(), likes.stream().map(l -> l.getId()).collect(Collectors.toList()));
					n_likes = (long) likes.size();
				} else
					n_likes = 0L;

				while( it.hasNext() ) {
					List<Like> likes = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Like.class)).collect(Collectors.toList());
					for (Like l : likes)
						Redis.LRUHyperLogUpdate(Redis.DAYLY_LIKES, p.getId(), l.getId(), false);

					n_likes += likes.size();
				}
			} else {
				String query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='" + p.getId() + "' AND l._ts>=" + time;
				List<String> likes = CosmosClient.query(PostResource.LIKES_CONTAINER, query);
				if (!likes.isEmpty()) {
					JsonElement root = JsonParser.parseString(likes.get(0));
					n_likes = root.getAsJsonObject().get("Likes").getAsLong();
				} else
					n_likes = 0L;
			}
		}

		n_likes = (n_likes == 0L ? 0L : (long)Math.log10(n_likes));
		n_replies = (n_replies == 0L ? 0L : (long)Math.log10(n_replies));

		int a = (int) Math.round(0.8 * n_likes + 0.2 * n_replies);
		int b = (int) Math.round(0.2 * n_likes + 0.8 * n_replies);
		int hotness = Math.max(a, b);

		return hotness;
	}

	private static int getTrending(PostWithReplies p) {
		boolean inCache = Redis.LRUDictionaryGet(Redis.TOP_POSTS, p.getId()) != null;
		return (inCache ? 1 : 0) * 100;
	}

	private static int getScore(PostWithReplies p) {
		int freshness = getFreshness(p);
		int hotness = getHotness(p);
		int popularity = getPopularity(p);
		int trending = getTrending(p);
		int score = (int)Math.round(0.12*popularity + 0.24*freshness + 0.24*trending + 0.4*hotness); // Utilizar também o nº de visualizações
		return score;
	}

}

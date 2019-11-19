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

import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedResponse;
import redis.clients.jedis.Jedis;
import scc.models.Like;
import scc.models.PostWithReplies;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.GSON;
import scc.utils.MyBase64;

@Path(PagesResource.PATH)
public class PagesResource {

	public static final String PATH = "/page";
	private static final String INITIAL_PAGE = "initial_page";
	private static final int DEFAULT_INITIAL_PAGE_SIZE = 10;
	private static final int DEFAULT_LEVEL = 3;
	private static final int DEFAULT_PAGE_SIZE = 5;
	//private static final int MAX_SIZE_ALLOWED = 2;

	@GET
	@Path("/thread/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public PostWithReplies getThread(@PathParam("id") String id, @DefaultValue(""+DEFAULT_LEVEL) @QueryParam("d") int depth, @DefaultValue(""+DEFAULT_PAGE_SIZE) @QueryParam("p") int pageSize, @QueryParam("t") String continuationToken) {

		if(continuationToken != null)
			continuationToken = MyBase64.decodeString(continuationToken);

		String post_json = PostResource.getPost(id);
		PostWithReplies post = GSON.fromJson(post_json, PostWithReplies.class);

		Queue<PostWithReplies> queue = new LinkedList<>();
		queue.add(post);
		int current_level = 0, amount_posts_current_level = 1;
		while(!queue.isEmpty()) { 
			PostWithReplies current_post = queue.poll();
			amount_posts_current_level--;

			List<PostWithReplies> replies = null;
			List<String> replies_json = Redis.LRUListGet(Redis.TOP_REPLIES, id);
			if(replies_json == null) {
				String query_replies = "SELECT * FROM %s p WHERE p.parent='" + current_post.getId() +"'";
				Entry<String,List<PostWithReplies>> entry = CosmosClient.queryAndUnparsePaginated(PostResource.CONTAINER, query_replies, continuationToken, pageSize, PostWithReplies.class);
				replies = entry.getValue();

			}
			current_post.setReplies(replies);
			current_post.setContinuationToken(MyBase64.encode(entry.getKey()));

			// TODO: fazer uma lpush com as replies e quando há uma nova reply, adicioná-la a essa lista.

			String post_id = current_post.getId();
			Long n_likes = Redis.LRUHyperLogGet(Redis.TOTAL_LIKES, post_id);
			if(n_likes == null) {
				String query_likes = "SELECT COUNT(c) as Likes FROM %s c WHERE c.post_id='" + current_post.getId() +"'";
				List<String> likes = CosmosClient.query(PostResource.LIKE_CONTAINER, query_likes); 
				if(!likes.isEmpty()) {
					JsonElement root = JsonParser.parseString(likes.get(0));
					n_likes = root.getAsJsonObject().get("Likes").getAsLong();
					// TODO: adicionar à cache
				} else {
					n_likes = 0L;
				}
			}
			current_post.setLikes(n_likes.intValue());

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
			List<String> fromCache = Redis.getList(INITIAL_PAGE, n_posts);
			if(fromCache!= null && !fromCache.isEmpty()){
				return fromCache.stream().map(d -> GSON.fromJson(d , PostWithReplies.class))
						.collect(Collectors.toList()); // TODO: POrque não guardar apenas o Json da lista logo? -> ara poder obter apenas parte da lista
			} else {
				Comparator<Entry<Integer, PostWithReplies>> comp = (x, y) -> x.getKey().compareTo(y.getKey());
				Queue<Entry<Integer, PostWithReplies>> queue = new PriorityQueue<Entry<Integer, PostWithReplies>>(n_posts, comp);

				String query = "SELECT * FROM %s p WHERE p.parent=null";
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(PostResource.CONTAINER, query);
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
				
				Redis.putInList(INITIAL_PAGE, list.stream().map(e -> GSON.toJson(e)).toArray(String[]::new));

				return list;
			}
		} catch(Exception e) {
			throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
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
		String query = "SELECT * FROM %s p WHERE p.parent='" + p.getId() + "'";
		List<PostWithReplies> replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);
		p.setReplies(replies);
		//	TODO: Ir buscar à cache

		// Total Likes
		Long total_likes = Redis.LRUHyperLogGet(Redis.TOTAL_LIKES, p.getId());
		if(total_likes == null) {
			if(Redis.ACTIVE) {
				query = "SELECT * FROM %s l WHERE l.post_id='" + p.getId() + "'";
				List<Like> likes = CosmosClient.queryAndUnparse(PostResource.LIKE_CONTAINER, query, Like.class);
				total_likes = (long) likes.size();
				Redis.LRUHyperLogPut(Redis.TOTAL_LIKES, Redis.TOTAL_LIKES_LIMIT, p.getId(), likes.stream().map(l -> GSON.toJson(l)).collect(Collectors.toList()));
			} else {
				query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='" + p.getId() + "'";
				List<String> likes = CosmosClient.query(PostResource.LIKE_CONTAINER, query);
				if (!likes.isEmpty()) {
					JsonElement root = JsonParser.parseString(likes.get(0));
					total_likes = root.getAsJsonObject().get("Likes").getAsLong();
				} else
					total_likes = 0L;
			}
		}
		p.setLikes(total_likes.longValue());

		int n_likes = p.getLikes() == 0 ? 0 :(int) Math.log10(p.getLikes());
		int n_replies = p.getReplies().size() == 0 ? 0 :(int) Math.log10(p.getReplies().size());
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
				List<PostWithReplies> replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);
				n_replies = (long) replies.size();
				Redis.LRUHyperLogPut(Redis.DAYLY_REPLIES, Redis.DAYLY_REPLIES_LIMIT, p.getId(), replies.stream().map(r -> GSON.toJson(r)).collect(Collectors.toList()));
			} else {
				String query = "SELECT COUNT(p) as Replies FROM %s p WHERE p.parent='" + p.getId() + "' AND p._ts>=" + time;
				List<String> replies = CosmosClient.query(PostResource.LIKE_CONTAINER, query);
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
				List<Like> likes = CosmosClient.queryAndUnparse(PostResource.LIKE_CONTAINER, query, Like.class);
				n_likes = (long) likes.size();
				Redis.LRUHyperLogPut(Redis.DAYLY_LIKES, Redis.DAYLY_LIKES_LIMIT, p.getId(), likes.stream().map(l -> GSON.toJson(l)).collect(Collectors.toList()));
			} else {
				String query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='" + p.getId() + "' AND l._ts>=" + time;
				List<String> likes = CosmosClient.query(PostResource.LIKE_CONTAINER, query);
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

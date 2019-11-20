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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@Path(PageResource.PATH)
public class PageResource {

	public static final String PATH = "/page";
	private static final int DEFAULT_INITIAL_PAGE_SIZE = 10;
	private static final int DEFAULT_LEVEL = 3;
	private static final int DEFAULT_PAGE_SIZE = 3;
	private static final int DEFAULT_PAGE_NUMBER = 1;
	//private static final int MAX_SIZE_ALLOWED = 2;
	
	Logger logger = LoggerFactory.getLogger(PageResource.class);

	@GET
	@Path("/thread/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getThread(@PathParam("id") String id, @DefaultValue(""+DEFAULT_LEVEL) @QueryParam("d") int depth, @DefaultValue(""+DEFAULT_PAGE_SIZE) @QueryParam("ps") int page_size, @QueryParam("t") String continuation_token) {

		String post_json = PostResource.getPost(id);
		PostWithReplies post = GSON.fromJson(post_json, PostWithReplies.class);

		Queue<PostWithReplies> queue = new LinkedList<>();
		queue.add(post);
		int current_level = 0, amount_posts_current_level = 1;
		while(!queue.isEmpty()) { 
			PostWithReplies current_post = queue.poll();
			amount_posts_current_level--;

			String post_id = current_post.getId();

			Entry<String, List<PostWithReplies>> entry = PostResource.getReplies(post_id, continuation_token, page_size);
			List<PostWithReplies> replies = entry.getValue();
			String next_continuation_token = entry.getKey();

			current_post.setReplies(replies);
			current_post.setContinuationToken(next_continuation_token);

			// Total Likes
			long total_likes = PostResource.getTotalLikes(post_id);
			current_post.setLikes(total_likes);

			if(current_level < depth) {
				queue.addAll(replies);
			}

			if(amount_posts_current_level == 0) {
				current_level++;
				amount_posts_current_level = queue.size();
			}
		}

		return GSON.toJson(post);
	}

	@GET
	@Path("/initial")
	@Produces(MediaType.APPLICATION_JSON)
	public String getInitialPage(@DefaultValue(""+DEFAULT_INITIAL_PAGE_SIZE) @QueryParam("ps") int page_size, @DefaultValue(""+DEFAULT_PAGE_NUMBER)  @QueryParam("p")  int page_number/*, @DefaultValue(""+MAX_SIZE_ALLOWED) @QueryParam("m") int max_size*/) {

		//TODO: verificar se o n_posts é menor que o valor que está a ser calculado pela função.

		// TODO: Porque não guardar apenas o Json da lista logo? -> ara poder obter apenas parte da lista
		try {
			List<String> fromCache = Redis.getPaginatedList(Redis.INITIAL_PAGE, page_size, page_number);
			if(fromCache!= null && !fromCache.isEmpty()){
				logger.info("Initial page retrieved from Cache: " + page_size + " posts/page p=" + page_number);
				List<PostWithReplies> requested_page = fromCache.stream().map( d -> GSON.fromJson(d , PostWithReplies.class) ).collect(Collectors.toList()); 
				return GSON.toJson(requested_page);
			} else {
				logger.info("Computing initial page: " + page_size + " posts/page p=" + page_number);
				
				int queue_size = page_size*page_number;
				Comparator<Entry<Integer, PostWithReplies>> comp = (x, y) -> x.getKey().compareTo(y.getKey());
				Queue<Entry<Integer, PostWithReplies>> queue = new PriorityQueue<Entry<Integer, PostWithReplies>>(queue_size, comp);

				String query = "SELECT * FROM %s p WHERE p.parent=null";
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(PostResource.POSTS_CONTAINER, query);
				while( it.hasNext() ) {
					Iterable<PostWithReplies> postsWithReplies = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), PostWithReplies.class)).collect(Collectors.toList());
					for (PostWithReplies p : postsWithReplies) {
						int score = getScore(p);
						logger.info("post: " + p.getId() + " score: " + score);
						
						if (queue.size() < queue_size) {
							queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(score, p));
						} else {
							Entry<Integer, PostWithReplies> e = queue.peek();
							if (queue.size() >= queue_size) {
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
				
				logger.info("Computed initial page");
				
				List<PostWithReplies> posts = queue.stream().map(e -> e.getValue()).collect(Collectors.toList());
				
			//	String[] posts = queue.stream().map(e -> GSON.toJson(e.getValue())).collect(Collectors.toList()).stream().toArray(String[]::new);
				
				Redis.del(Redis.INITIAL_PAGE);
				Redis.putInList(Redis.INITIAL_PAGE, posts.stream().map( p -> GSON.toJson(p) ).toArray(String[]::new));

				List<PostWithReplies> requested_page = posts.subList((page_number-1)*page_size, (page_number*page_size)+1);
				
//				List<String> requested_page = Arrays.asList(Arrays.copyOfRange(posts, (page_number-1)*page_size, (page_number*page_size)+1));
				
				return GSON.toJson(requested_page);
			}
		} catch(Exception e) {
			throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}

	public static int getFreshness(PostWithReplies p) {
		double days = ((System.currentTimeMillis() / 1000) - p.getCreationTime() + 1) / ((double)( 24 * 60 * 60 ));

		if(days < 0.3)
			days = 0.3;

		int freshness = (int)Math.round(100.0/days);
		return freshness;
	}

	public static int getPopularity(PostWithReplies p) {

		// Total Replies
		long total_replies = PostResource.getTotalReplies(p.getId(), p, DEFAULT_PAGE_SIZE);

		// Total Likes
		long total_likes = PostResource.getTotalLikes(p.getId());
		p.setLikes(total_likes);

		// In our data, these values are too low to use the logarithm.
//		total_likes = total_likes == 0 ? 0 : Math.log10(total_likes);
//		total_replies = total_replies == 0 ? 0 : Math.log10(total_replies);
		int a = (int) Math.round(0.8 * total_likes + 0.2 * total_replies);
		int b = (int) Math.round(0.2 * total_likes + 0.8 * total_replies);
		int popularity = Math.max(a, b);
		return popularity;
	}

	public static int getHotness(PostWithReplies p) {
		long time = (System.currentTimeMillis() / 1000) - (24 * 60 * 60);

		// Replies in last 24h
		Long n_replies = PostResource.getRecentReplies(p.getId(), time);

		// Likes in last 24h
		Long n_likes = PostResource.getRecentLikes(p.getId(), time);

//		n_likes = (n_likes == 0L ? 0L : (long)Math.log10(n_likes));
//		n_replies = (n_replies == 0L ? 0L : (long)Math.log10(n_replies));

		int a = (int) Math.round(0.8 * n_likes + 0.2 * n_replies);
		int b = (int) Math.round(0.2 * n_likes + 0.8 * n_replies);
		int hotness = Math.max(a, b);

		return hotness;
	}

	public static int getTrending(PostWithReplies p) {
		boolean inCache = Redis.LRUDictionaryGet(Redis.TOP_POSTS, p.getId()) != null;
		return (inCache ? 1 : 0) * 100;
	}

	public static int getScore(PostWithReplies p) {
		int freshness = getFreshness(p);
		int hotness = getHotness(p);
		int popularity = getPopularity(p);
		int trending = getTrending(p);
		int score = (int)Math.round(0.12*popularity + 0.24*freshness + 0.24*trending + 0.4*hotness); // Utilizar também o nº de visualizações
		return score;
	}

}

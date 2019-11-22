package scc.resources;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedResponse;

import scc.models.PostWithReplies;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.Config;
import scc.utils.GSON;

public class PageResource {
	
	static Logger logger = LoggerFactory.getLogger(PageResource.class);

	public static PostWithReplies getThread(String id, int depth, int page_size, String continuation_token) {
		
		PostWithReplies post = new PostWithReplies(PostResource.get(id));

		Queue<PostWithReplies> queue = new LinkedList<>();
		queue.add(post);
		int current_level = 0, amount_posts_current_level = 1;
		while(!queue.isEmpty()) { 
			PostWithReplies current_post = queue.poll();
			amount_posts_current_level--;

			logger.info(GSON.toJson(current_post));
			
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

		return post;
	}

	public static List<PostWithReplies> getInitialPage(int page_size, int page_number) {
		
		int max_posts = Integer.parseInt(Config.getSystemProperty(Config.MAX_INITIAL_PAGE_POSTS));
		int max_page = (max_posts / page_size) + ( max_posts % page_size > 0 ? 1 : 0 );
		if(page_number > max_page)
			throw new WebApplicationException( Response.status(Status.BAD_REQUEST).entity("Invalid page!").build() );

		// TODO: Porque não guardar apenas o Json da lista? -> para poder obter apenas parte da lista
		try {
			List<String> fromCache = Redis.getPaginatedList(Config.INITIAL_PAGE, page_size, page_number);
			if(fromCache!= null && !fromCache.isEmpty()){
				logger.info("Initial page retrieved from Cache: " + page_size + " posts/page p=" + page_number);
				List<PostWithReplies> requested_page = fromCache.stream().map( d -> GSON.fromJson(d , PostWithReplies.class) ).collect(Collectors.toList()); 
				return requested_page;
			} else {
				List<PostWithReplies> initial_page = computeInitialPage(page_size, page_number);
				Redis.del(Config.INITIAL_PAGE);
				Redis.putInList(Config.INITIAL_PAGE, initial_page.stream().map( p -> GSON.toJson(p) ).toArray(String[]::new));

				List<PostWithReplies> requested_page = initial_page.subList((page_number-1)*page_size,
						Math.min((page_number*page_size), Integer.parseInt(Config.getSystemProperty(Config.MAX_INITIAL_PAGE_POSTS))));
				return requested_page;
			}
		} catch(Exception e) {
			e.printStackTrace();
			throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}
	
	public static List<PostWithReplies> computeInitialPage(int page_size, int page_number) {

		logger.info("Computing initial page: " + page_size + " posts/page p=" + page_number);

		int queue_size = Math.min(page_size*page_number, Integer.parseInt(Config.getSystemProperty(Config.MAX_INITIAL_PAGE_POSTS)));
		Comparator<Entry<Integer, PostWithReplies>> comp = (x, y) -> x.getKey().compareTo(y.getKey());
		Queue<Entry<Integer, PostWithReplies>> queue = new PriorityQueue<Entry<Integer, PostWithReplies>>(queue_size, comp);

		String query = "SELECT * FROM %s p WHERE p.parent=null";
		Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(Config.POSTS_CONTAINER, query);
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

		return posts;
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
		long total_replies = PostResource.getTotalReplies(p.getId(), p, Integer.parseInt(Config.getSystemProperty(Config.DEFAULT_REPLIES_PAGE_SIZE)));

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
		boolean inCache = Redis.LRUDictionaryGet(Config.TOP_POSTS, p.getId()) != null;
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

package scc.controllers;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.PriorityQueue;
import java.util.Queue;

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

import com.microsoft.azure.cosmosdb.FeedOptions;
import scc.models.PostWithReplies;
import scc.storage.CosmosClient;
import scc.storage.Redis;

@Path(PagesResource.PATH)
public class PagesResource {

	private static final int DEFAULT_INITIAL_PAGE_SIZE = 10;
	static final String PATH = "/page";
	static final int DEFAULT_LEVEL = 3;
	private static final String INITIAL_PAGE = "initial_page";

	@GET
	@Path("/thread/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public PostWithReplies getThread(@PathParam("id") String id, @DefaultValue(""+DEFAULT_LEVEL) @QueryParam("d") int depth) {

		PostWithReplies post = CosmosClient.getByIdUnparse(PostResource.CONTAINER, id, PostWithReplies.class);
		if(post == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("Post does not exists").build() );

		Queue<PostWithReplies> queue = new LinkedList<>();
		queue.add(post);
		int current_level = 0, amount_posts_current_level = 1;
		while(!queue.isEmpty()) {
			PostWithReplies current_post = queue.poll();
			amount_posts_current_level--;

			String query_replies = "SELECT * FROM %s p WHERE p.parent='" + current_post.getId() +"'";
			List<PostWithReplies> replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query_replies, PostWithReplies.class);
			current_post.setReplies(replies);

			String query_likes = "SELECT COUNT(c) as Likes FROM %s c WHERE c.post_id='" + current_post.getId() +"'";
			List<String> likes = CosmosClient.query(PostResource.LIKE_CONTAINER, query_likes); 
			if(!likes.isEmpty()) {
				JsonElement root = new JsonParser().parse(likes.get(0));
				int n_likes = root.getAsJsonObject().get("Likes").getAsInt();
				current_post.setLikes(n_likes);
			}

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
	public List<PostWithReplies> getInitialPage(@DefaultValue(""+DEFAULT_INITIAL_PAGE_SIZE) @QueryParam("p") int n_posts) {

		try {
			Comparator<Entry<Integer, PostWithReplies>> comp = (x, y) -> x.getKey().compareTo(y.getKey());
			Queue<Entry<Integer, PostWithReplies>> queue = new PriorityQueue<Entry<Integer, PostWithReplies>>(n_posts, comp);

			long time = System.currentTimeMillis() - 24*60*60*1000;

			String query = "SELECT * FROM %s p WHERE p.parent=null";
			List<PostWithReplies> posts = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);
			for(PostWithReplies p : posts) {
				// Replies in last 24h
				query = "SELECT * FROM %s p WHERE p.parent='" + p.getId() + "' AND p.creationTime>=" + time;
				List<PostWithReplies> replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);
				p.setReplies(replies);

				// Também se pode ir ver as replies das replies ....

				// Likes in last 24h
				query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='"+ p.getId() + "' AND l.creationTime>=" + time;
				List<String> likes = CosmosClient.query(PostResource.LIKE_CONTAINER, query);
				if(!likes.isEmpty()) {
					JsonElement root = new JsonParser().parse(likes.get(0));
					int n_likes = root.getAsJsonObject().get("Likes").getAsInt();
					p.setLikes(n_likes);
				}

				// Visualizations(?)

				//int hotness = (int) Math.round(0.5*p.getLikes() + 0.5*p.getReplies().size());
				int hotness = Math.max((int) Math.round(0.8*p.getLikes() + 0.2*p.getReplies().size()),
						(int) Math.round(0.2*p.getLikes() + 0.8*p.getReplies().size()));
				if(queue.size() < n_posts) {
					queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(hotness, p));
				} else {
					Entry<Integer, PostWithReplies> e = queue.peek();
					if(queue.size() >= n_posts) { // TODO: Query A
						if(e.getKey() < hotness) {
							queue.poll();
							queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(hotness, p));
						} else if(e.getKey() == hotness) {
							queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(hotness, p));
						}
					}
				}
			}

			List<PostWithReplies> list = queue.stream().map(e -> e.getValue()).collect(Collectors.toList());

			return list;
		} catch(Exception e) {
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}


	}

}

package scc.controllers;

import java.util.LinkedList;
import java.util.List;
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

import scc.models.PostWithReplies;
import scc.storage.CosmosClient;

@Path(PagesResource.PATH)
public class PagesResource {

	static final String PATH = "/page";
	static final int DEFAULT_LEVEL = 3;

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
	public String getInitialPage() {
		// TODO
		return "HeLLo!";
	}
}

package scc.controllers;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import scc.models.Post;
import scc.models.PostWithReplies;
import scc.storage.CosmosClient;
import scc.storage.GSON;

@Path(PagesResource.PATH)
public class PagesResource {

	static final String PATH = "/page";
	static final int DEFAULT_LEVEL = 1;

	@GET
	@Path("/thread/{id}/")
	@Produces(MediaType.APPLICATION_JSON)
	public Post getThread(@PathParam("id") String id, @QueryParam("d") Integer depth) {

		PostWithReplies post = CosmosClient.getByIdUnparse(PostResource.CONTAINER, id, PostWithReplies.class);
		if(post == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("Post does not exists").build());

		//String query = "SELECT * FROM %s p WHERE p.parent='" + post.getId() +"'";

		//List<PostWithReplies> replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);

		//post.setReplies(replies);

		/*for(PostWithReplies r : replies) {
			query = "SELECT * FROM %s p WHERE p.parent='" + r.getId() +"'";

			List<PostWithReplies> inner_replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);
			r.setReplies(inner_replies);
		}*/

		Queue<PostWithReplies> queue = new LinkedList<>();
		queue.add(post);
		int current_level = 0, max_level = (depth == null) ? DEFAULT_LEVEL : depth.intValue(), amount_posts_current_level = 1;
		// TODO: receber como queryParam
		while(!queue.isEmpty()) {
			PostWithReplies current_post = queue.poll();
			amount_posts_current_level--;

			String query = "SELECT * FROM %s p WHERE p.parent='" + current_post.getId() +"'";
			List<PostWithReplies> replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);
			current_post.setReplies(replies);

			// TODO: Ir buscar os likes
			
			if(current_level < max_level) {
				queue.addAll(replies);
			}
			
			if(amount_posts_current_level == 0) {
				current_level++;
				amount_posts_current_level = queue.size();
			}
		}

		return post;
	}

}

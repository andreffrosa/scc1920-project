package scc.controllers;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
	public Post getThread(@PathParam("id") String id) {

		String post_json = CosmosClient.getById(PostResource.CONTAINER, id);
		if(post_json == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("Post does not exists").build());

		PostWithReplies post = GSON.fromJson(post_json, PostWithReplies.class);

		String query = "SELECT * FROM %s p WHERE p.parent='" + post.getId() +"'";

		List<PostWithReplies> replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);

		post.setReplies(replies);
		
		/*for(PostWithReplies r : replies) {
			query = "SELECT * FROM %s p WHERE p.parent='" + r.getId() +"'";
			
			List<PostWithReplies> inner_replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);
			r.setReplies(inner_replies);
		}*/
		
		return post;

	}

}

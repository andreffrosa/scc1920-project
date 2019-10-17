package scc.controllers;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import scc.models.Post;
import scc.storage.CosmosClient;
import scc.storage.GSON;

@Path(PagesResource.PATH)
public class PagesResource {

	static final String PATH = "/page";

	@GET
	@Path("/thread/{id}/")
	@Produces(MediaType.APPLICATION_JSON)
	public Post getThread(@PathParam("id") String id) {

		String post_json = CosmosClient.getById(PostResource.CONTAINER, id);
		if(post_json == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("Post does not exists").build());

		Post post = GSON.fromJson(post_json, Post.class);

		String query = "SELECT * FROM %s p WHERE p.parent='" + post.getId() +"'";

		List<String> replies = CosmosClient.query(PostResource.CONTAINER, query);

		post.setTitle(String.format(query, PostResource.CONTAINER));
		post.setAuthor(replies.size() +  "");
		//post.setReplies(replies);

		return post;

	}

}

package scc.controllers;

import com.microsoft.azure.cosmosdb.DocumentClientException;
import scc.models.Like;
import scc.models.Post;
import scc.storage.CosmosClient;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

@Path(PostResource.PATH)
public class PostResource extends Resource{

	static final String PATH = "/post";
	private static final String CONTAINER = "Posts";

	@Context
	static ServletContext context;

	public PostResource() throws Exception {
		super(CONTAINER);
	}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Post post){

		String community = CosmosClient.getByName(CommunityResource.CONTAINER, post.getCommunity());
		if(community == null)
			throw new WebApplicationException("Community does not exist", Status.NOT_FOUND);

		try{
			return super.create(post);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException("Post with that ID already exists", Status.CONFLICT);
			else
				throw new WebApplicationException("Unexpected error", Status.INTERNAL_SERVER_ERROR);
		}
	}

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String findById(@PathParam("id") String id){
        String post = super.getById(id);

        if (post == null)
        	throw new WebApplicationException("Post with that ID does not exist", Status.NOT_FOUND);

        return post;
    }


    @POST
	@Path("/{id}/like/{user_id}")
	public String likePost(@PathParam("id") String postId, @PathParam("user_id") String user_id){
		String post = CosmosClient.getById(CONTAINER, postId);
		if(post == null)
			throw new WebApplicationException("Post does not exist", Status.NOT_FOUND);

		try {
			return super.create(new Like(postId, user_id));
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException("You have already liked that post", Status.CONFLICT.getStatusCode());
			else
				throw new WebApplicationException("Unexpected error", Status.INTERNAL_SERVER_ERROR);
		}
	}

    /*@PUT
    @Path("/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(Post u){
        return super.update(u);
    }

    @DELETE
    @Path("/{name}")
    public Response delete(@PathParam("name") String name){
        return super.delete(name);
    }*/
}

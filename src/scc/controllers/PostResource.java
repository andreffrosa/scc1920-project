package scc.controllers;

import com.microsoft.azure.cosmosdb.DocumentClientException;
import scc.models.Like;
import scc.models.Post;
import scc.storage.CosmosClient;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path(PostResource.PATH)
public class PostResource extends Resource{

	static final String PATH = "/post";
	public static final String CONTAINER = "Posts";

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

		String author = CosmosClient.getByName(UserResouce.CONTAINER, post.getAuthor());
		if(author == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("Username does not exists").build());

		String community = CosmosClient.getByName(CommunityResource.CONTAINER, post.getCommunity());
		if(community == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Community does not exist").build());

		try{
			post.setCreationTime(System.currentTimeMillis());
			return super.create(post);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity("Post with that ID already exists").build());
			else
				throw new WebApplicationException( Response.serverError().entity("Unexpected error").build());
		}
	}

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String findById(@PathParam("id") String id){
        String post = super.getById(id);

        if (post == null)
        	throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("Post with that ID does not exist").build());

        return post;
    }


    @POST
	@Path("/{id}/like/{user_name}")
	public String likePost(@PathParam("id") String postId, @PathParam("user_name") String user_name){

		String author = CosmosClient.getByName(UserResouce.CONTAINER, user_name);
		if(author == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Username does not exists").build());

		String post = CosmosClient.getById(CONTAINER, postId);
		if(post == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Post does not exist").build());

		try {
			Like like = new Like(postId, user_name, System.currentTimeMillis());
			return super.create(like);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity("You have already liked that post").build());
			else
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity("Unexpected error").build());
		}
	}

	@DELETE
	@Path("/{id}/dislike/{user_name}")
	public void dislikePost(@PathParam("id") String postId, @PathParam("user_name") String user_name){

		String author = CosmosClient.getByName(UserResouce.CONTAINER, user_name);
		if(author == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Username does not exists").build());

		String post = CosmosClient.getById(CONTAINER, postId);
		if(post == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Post does not exist").build());

		try {
			super.delete(Like.buildId(postId, user_name));
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity("You have already liked that post").build());
			else
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity("Unexpected error").build());
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

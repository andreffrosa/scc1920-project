package scc.controllers;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.cosmosdb.DocumentClientException;

import scc.models.Like;
import scc.models.Post;
import scc.storage.CosmosClient;
import scc.storage.Redis;

@Path(PostResource.PATH)
public class PostResource extends Resource{

	public static final String PATH = "/post";
	public static final String CONTAINER = "Posts";
	public static final String LIKE_CONTAINER = "Likes";

	public PostResource() throws Exception {
		super(CONTAINER);
	}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Post post){

		if(!post.validPost())
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Params").build());

		String author = CosmosClient.getByName(UserResource.CONTAINER, post.getAuthor());
		if(author == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("Username does not exists").build());

		String community = CosmosClient.getByName(CommunityResource.CONTAINER, post.getCommunity());
		if(community == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Community does not exist").build());

		try{
			return super.create(post);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity("Post with that ID already exists").build());
			else
				throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
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
	public String likePost(@PathParam("id") String post_id, @PathParam("user_name") String user_name){

		if(!UserResource.userExists(user_name))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Username does not exists").build());

		String post = CosmosClient.getById(CONTAINER, post_id);
		if(post == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Post does not exist").build());

		try {
			Like like = new Like(post_id, user_name);
			String likeId = CosmosClient.create(LIKE_CONTAINER, like);
			Redis.increment(likeId);
			return likeId;
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity("You have already liked that post").build() );
			else
				throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}

	@DELETE
	@Path("/{id}/dislike/{user_name}")
	public void dislikePost(@PathParam("id") String postId, @PathParam("user_name") String user_name){

		String author = CosmosClient.getByName(UserResource.CONTAINER, user_name);
		if(author == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Username does not exists").build());

		String post = CosmosClient.getById(CONTAINER, postId);
		if(post == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Post does not exist").build());

		try {
			CosmosClient.delete(LIKE_CONTAINER, Like.buildId(postId, user_name));
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity("You have already liked that post").build()); // TODO: esta msg está mal
			else
				throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}
}

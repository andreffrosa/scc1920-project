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
import scc.utils.GSON;

@Path(PostResource.PATH)
public class PostResource extends Resource{

	public static final String PATH = "/post";
	public static final String CONTAINER = "Posts";
	public static final String LIKE_CONTAINER = "Likes";
	private static final int MAX_RECENT_POSTS = 100;
	public static final String MOST_RECENT_POSTS = "MostRecentPosts";

	public PostResource() throws Exception {
		super(CONTAINER);
	}
	
    public static boolean exists(String post_id) {
		return CosmosClient.getById(CONTAINER, post_id) != null;
	}
    
    public static boolean existsLike(String like_id) {
    	return CosmosClient.getById(LIKE_CONTAINER, like_id) != null;
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Post post){
		if(!post.validPost())
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Parameters").build());

		if(!UserResource.exists(post.getAuthor()))
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity(String.format("Username %s does not exist", post.getAuthor())).build());
		
		if(!CommunityResource.exists(post.getCommunity()))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Community %s does not exist", post.getCommunity())).build());

		//TODO: Se parent != null, verificar se o post ainda existe
		
		try{
			return CosmosClient.create(super.collection, post);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity("Post with that ID already exists").build());
			else
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPost(@PathParam("id") String id){

		Post post = CosmosClient.getByNameUnparse(super.collection, id, Post.class);

        if (post == null)
        	throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity(String.format("Post %s does not exist.", id)).build());

        String toReturn = GSON.toJson(post);
        Redis.putInBoundedList(MOST_RECENT_POSTS, MAX_RECENT_POSTS, toReturn);
        return toReturn;
    }
    
    @POST
	@Path("/{id}/like/{username}")
	public String likePost(@PathParam("id") String post_id, @PathParam("username") String username){

		if(!UserResource.exists(username))
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity(String.format("Username %s does not exist", username)).build());

		if(!PostResource.exists(post_id))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Post %p does not exist", post_id)).build());

		try {
			Like like = new Like(post_id, username);
			String like_id = CosmosClient.create(LIKE_CONTAINER, like);
			Redis.addToHyperLog(like_id, like_id);
			return like_id;
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity(String.format("User %s have already liked that post", username)).build() );
			else
				throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}

	@DELETE
	@Path("/{id}/dislike/{username}")
	public void dislikePost(@PathParam("id") String post_id, @PathParam("username") String username){

		String like_id = Like.buildId(post_id, username);
		
		if(!PostResource.existsLike(like_id))
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity(String.format("User %s have not yet liked that post", username)).build());
		
		try {
			CosmosClient.delete(LIKE_CONTAINER, like_id);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity(String.format("User %s have already liked post %s", username, post_id)).build() );
			else
				throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}
}

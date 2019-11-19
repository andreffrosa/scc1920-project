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
public class PostResource extends Resource {

	public static final String PATH = "/post";
	public static final String CONTAINER = "Posts";
	public static final String LIKE_CONTAINER = "Likes";

	public static boolean exists(String post_id) {
		try {
			return getPost(post_id) != null;
		} catch(WebApplicationException e) {
			return false;
		}
	}

	// TODO: Este não vale a pena ir à cache porque não é uma operação lá muito frequente
	public static boolean existsLike(String like_id) {
		return CosmosClient.getById(LIKE_CONTAINER, like_id) != null;
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public static String create(Post post) {
		try {
			if (!post.validPost())
				throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Parameters").build());

			if (!UserResource.exists(post.getAuthor()))
				throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Username %s does not exist", post.getAuthor())).build());

			if (!CommunityResource.exists(post.getCommunity()))
				throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Community %s does not exist", post.getCommunity())).build());

			if (post.isReply()) {
				if (post.validReply()) {
					String post_id = CosmosClient.insert(CONTAINER, post);

					// Update cache
					if(post.getParent() != null) {
						Redis.LRUHyperLogUpdate(Redis.TOTAL_REPLIES, post.getParent(), post_id, false);
						Redis.LRUHyperLogUpdate(Redis.DAYLY_REPLIES, post.getParent(), post_id, false);
						
						//Redis.LRUListUpdate(Redis.TOP_REPLIES, post.getParent(), GSON.toJson(post), false); // TODO: Não pode ser isto
					}

					return post_id;
				} else
					throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Parameters").build());
			} else
				return CosmosClient.insert(CONTAINER, post);
		} catch (DocumentClientException e) {
			if (e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity("Post with that ID already exists").build());
			else
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}

	@GET
	@Path("/{post_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public static String getPost(@PathParam("post_id") String post_id) {
		String post_json = Redis.LRUDictionaryGet(Redis.TOP_POSTS, post_id);
		if(post_json == null) {
			Post post = CosmosClient.getByNameUnparse(CONTAINER, post_id, Post.class);
			
			if (post == null)
				throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Post %s does not exist.", post_id)).build());
			
			post_json = GSON.toJson(post);
			
			Redis.LRUDictionaryPut(Redis.TOP_POSTS, Redis.TOP_POSTS_LIMIT, post_id, post_json);
		}

		return post_json;
	}

	@POST
	@Path("/{id}/like/{username}")
	public static String likePost(@PathParam("id") String post_id, @PathParam("username") String username) {

		if (!UserResource.exists(username))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Username %s does not exist", username)).build());

		if (!PostResource.exists(post_id))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Post %s does not exist", post_id)).build());

		try {
			Like like = new Like(post_id, username);
			String like_id = CosmosClient.insert(LIKE_CONTAINER, like);

			// If in cache, update
			Redis.LRUHyperLogUpdate(Redis.TOTAL_LIKES, post_id, like_id, false);
			Redis.LRUHyperLogUpdate(Redis.DAYLY_LIKES, post_id, like_id, false);

			return like_id;
		} catch (DocumentClientException e) {
			if (e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity(String.format("User %s have already liked post %s", username, post_id)).build());
			else
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}

	@DELETE
	@Path("/{id}/dislike/{username}")
	public static void dislikePost(@PathParam("id") String post_id, @PathParam("username") String username) {

		String like_id = Like.buildId(post_id, username);

		if (!PostResource.existsLike(like_id))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("User %s have not yet liked that post %s", username, post_id)).build());

		try {
			CosmosClient.delete(LIKE_CONTAINER, like_id);

			// If in cache, set dirty bit to true
			Redis.LRUSetDirtyBit(Redis.TOTAL_LIKES, post_id, true);
			Redis.LRUSetDirtyBit(Redis.DAYLY_LIKES, post_id, true);
			
		} catch (DocumentClientException e) {
			if (e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity(String.format("User %s have already disliked post %s", username, post_id)).build());
			else
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}
}

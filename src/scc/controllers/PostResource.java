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
	private static final int MAX_RECENT_POSTS = 100;
	public static final String MOST_RECENT_POSTS = "MostRecentPosts";

	/* public PostResource() throws Exception {
        super();
    }*/
	
	// TODO: Ir à cache ver isto
	public static boolean exists(String post_id) {
		return CosmosClient.getById(CONTAINER, post_id) != null;
	}

	// Este não vale a pena ir à cache porque não é uma operação lá muito frequente
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
						Redis.LRUHyperLogPut(Redis.TOTAL_REPLIES, Redis.TOTAL_REPLIES_LIMIT, post.getParent(), post_id);
						Redis.LRUHyperLogPut(Redis.DAYLY_REPLIES, Redis.DAYLY_REPLIES_LIMIT, post.getParent(), post_id);
						// TODO: potencial problema -> quando se mete apenas 1 like (e não está em cache) vai para a cache e depois é removido (quando excede o tamanho)
					}

					return post_id;
				} else
					throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Parameters").build());
			}else
				return CosmosClient.insert(CONTAINER, post);
		} catch (DocumentClientException e) {
			if (e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity("Post with that ID already exists").build());
			else
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public static String getPost(@PathParam("id") String id) {

		Post post = CosmosClient.getByNameUnparse(CONTAINER, id, Post.class);

		if (post == null)
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("Post %s does not exist.", id)).build());

		String toReturn = GSON.toJson(post);
		Redis.putInBoundedList(MOST_RECENT_POSTS, MAX_RECENT_POSTS, toReturn); // TODO: Isto já não existe
		return toReturn;
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

			// Update cache
			Redis.LRUHyperLogPut(Redis.TOTAL_LIKES, Redis.TOTAL_LIKES_LIMIT, post_id, like_id);
			Redis.LRUHyperLogPut(Redis.DAYLY_LIKES, Redis.DAYLY_LIKES_LIMIT, post_id, like_id);
			// TODO: potencial problema -> quando se mete apenas 1 like (e não está em cache) vai para a cache e depois é removido (quando excede o tamanho)

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

		// TODO: Utilizar um dirty bit para saber quais os Hyperlogs que valem a pena resetar -> aqueles que tenham tido algum dislike nas ultimas 24h.

		String like_id = Like.buildId(post_id, username);

		if (!PostResource.existsLike(like_id))
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(String.format("User %s have not yet liked that post %s", username, post_id)).build());

		try {
			CosmosClient.delete(LIKE_CONTAINER, like_id);
		} catch (DocumentClientException e) {
			if (e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity(String.format("User %s have already disliked post %s", username, post_id)).build());
			else
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build());
		}
	}
}

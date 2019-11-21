package scc.endpoints;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import scc.models.Post;
import scc.resources.PostResource;
import scc.utils.GSON;

@Path(PostEndpoint.PATH)
public class PostEndpoint {

	public static final String PATH = "/post";

	public PostEndpoint () {
		super();
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(Post post) {
		return PostResource.create(post);
	}

	@GET
	@Path("/{post_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public String get(@PathParam("post_id") String post_id) {
		return GSON.toJson(PostResource.get(post_id));
	}

	@POST
	@Path("/{id}/like/{username}")
	public String like(@PathParam("id") String post_id, @PathParam("username") String username) {
		return PostResource.likePost(post_id, username);
	}

	@DELETE
	@Path("/{id}/dislike/{username}")
	public void dislike(@PathParam("id") String post_id, @PathParam("username") String username) {
		PostResource.dislikePost(post_id, username);
	}

}

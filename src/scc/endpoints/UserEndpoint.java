package scc.endpoints;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import scc.models.User;
import scc.resources.UserResource;
import scc.utils.GSON;

@Path(UserEndpoint.PATH)
public class UserEndpoint {

	public static final String PATH = "/user";

	public UserEndpoint() {
		super();
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(User user){
		return UserResource.create(user);
	}

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public String get(@PathParam("name") String name){
		return GSON.toJson(UserResource.get(name));
	}

}

package scc.endpoints;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import scc.models.Community;
import scc.resources.CommunityResource;
import scc.utils.GSON;

@Path(CommunityEndpoint.PATH)
public class CommunityEndpoint {

	public static final String PATH = "/community";

	public CommunityEndpoint() {
		super();
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String newCommunity(Community c) {
		return CommunityResource.create(c);
	}

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getCommunity(@PathParam("name") String name) {
		return GSON.toJson(CommunityResource.get(name));
	}
	
}


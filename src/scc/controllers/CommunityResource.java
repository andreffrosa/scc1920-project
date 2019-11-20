package scc.controllers;

import javax.ws.rs.Consumes;
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

import scc.models.Community;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.GSON;

@Path(CommunityResource.PATH)
public class CommunityResource extends Resource {

	public static final String PATH = "/community";
	public static final String CONTAINER = "Communities";

	public CommunityResource() {
		super();
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String newCommunity(Community c) {
		return createCommunity(c);
	}

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getCommunity(@PathParam("name") String name) {
		return consultCommunity(name);
	}
	
	public static String createCommunity(Community c) {
		if(!c.isValid())
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Parameters").build());

		try {
			return CosmosClient.insert(CONTAINER, c);
		} catch(DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity(String.format("Community %s already exist in the system.", c.getName())).build());

			throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}
	
	public static String consultCommunity(@PathParam("name") String name) {
		String community_json = Redis.LRUDictionaryGet(Redis.TOP_COMMUNITIES, name);
		if(community_json == null) {
			Community community = CosmosClient.getByNameUnparse(CONTAINER, name, Community.class);

			if(community == null)
				throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity(String.format("Community %s does not exist.", name)).build());
			
			community_json = GSON.toJson(community);
			
			Redis.LRUDictionaryPut(Redis.TOP_COMMUNITIES, Redis.TOP_COMMUNITIES_LIMIT, name, community_json);
		}

		return community_json;
	}

	public static boolean exists(String name) {
		try {
			return consultCommunity(name) != null;
		} catch(WebApplicationException e) {
			return false;
		}
	}
}


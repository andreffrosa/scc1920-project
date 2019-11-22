package scc.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.cosmosdb.DocumentClientException;

import scc.models.Community;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.Config;
import scc.utils.GSON;

public class CommunityResource {

	public static String create(Community c) {
		if(!c.isValid())
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Parameters").build());

		try {
			return CosmosClient.insert(Config.COMMUNITIES_CONTAINER, c);
		} catch(DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity(String.format("Community %s already exist in the system.", c.getName())).build());

			throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}
	
	public static Community get(String name) {
		
		String community_json = Redis.LRUDictionaryGet(Config.TOP_COMMUNITIES, name);
		if(community_json == null) {
			Community community = CosmosClient.getByNameUnparse(Config.COMMUNITIES_CONTAINER, name, Community.class);

			if(community == null)
				throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity(String.format("Community %s does not exist.", name)).build());
			
			community_json = GSON.toJson(community);
			
			Redis.LRUDictionaryPut(Config.TOP_COMMUNITIES, Integer.parseInt(Config.getRedisProperty(Config.TOP_COMMUNITIES_LIMIT)), name, community_json);
			
			return community;
		}

		return GSON.fromJson(community_json, Community.class);
	}

	public static boolean exists(String name) {
		try {
			return get(name) != null;
		} catch(WebApplicationException e) {
			return false;
		}
	}
}


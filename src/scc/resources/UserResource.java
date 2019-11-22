package scc.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.cosmosdb.DocumentClientException;

import scc.models.User;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.Config;
import scc.utils.GSON;

public class UserResource {

	public static String create(User user){
		if(!user.isValid())
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Parameters").build());

		try {
			return CosmosClient.insert(Config.USERS_CONTAINER, user);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity(String.format("User %s already exists", user.getName())).build() );
			else
				throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}
	
	public static User get(String name){
		String user_json = Redis.LRUDictionaryGet(Config.TOP_USERS, name);
		if(user_json == null) {
			User user = CosmosClient.getByNameUnparse(Config.USERS_CONTAINER, name, User.class);

			if(user == null)
				throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity(String.format("User %s not found", name)).build());
			
			user_json = GSON.toJson(user);
			
			Redis.LRUDictionaryPut(Config.TOP_USERS, Integer.parseInt(Config.getRedisProperty(Config.TOP_USERS_LIMIT)), name, user_json);
			
			return user;
		}

		return GSON.fromJson(user_json, User.class);
	}
	
	public static boolean exists(String username) {		
		try {
			return get(username) != null;
		} catch(WebApplicationException e) {
			return false;
		}
	}

}

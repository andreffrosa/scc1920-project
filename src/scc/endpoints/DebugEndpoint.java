package scc.endpoints;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedResponse;

import scc.models.Like;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.Config;
import scc.utils.GSON;

@Path(DebugEndpoint.PATH)
public class DebugEndpoint {

	public static final String PATH = "/debug";

	public static final String VERSION = "100.0.0-r3 alfa-snapshot-0.0.0.0.0.1 SilkyX-Vanilla Final Edition ;0x";

	static Logger logger = LoggerFactory.getLogger(DebugEndpoint.class);

	@GET
	@Path("/version")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVersion(){
		logger.info(VERSION);
		return Response.ok(VERSION).build();
	}

	@DELETE
	@Path("/container/{container}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("container") String container, String query, @PathParam("key") String key){

		int deleted = CosmosClient.delete(container, query, key);

		logger.info("Deleted: " + deleted);
		return Response.ok("Deleted: " + deleted).build();
	}

	@DELETE
	@Path("/cache")
	@Produces(MediaType.APPLICATION_JSON)
	public Response clearCache() {
		String result = Redis.clear();
		logger.info("Cleared cache: " + result);
		return Response.ok(result).build();
	}
	
	@DELETE
	@Path("/cosmos")
	@Produces(MediaType.APPLICATION_JSON)
	public Response clearCosmos() {
		
		String query = "SELECT * FROM %s c";
		
		int communities = CosmosClient.delete(Config.COMMUNITIES_CONTAINER, query, "name");
		int users = CosmosClient.delete(Config.USERS_CONTAINER, query, "name");
		int posts = CosmosClient.delete(Config.POSTS_CONTAINER, query, "community");
		int likes = CosmosClient.delete(Config.LIKES_CONTAINER, query, "post_id");

		String result = String.format("Communities:\t%d\nUsers:\t%d\nPosts:\t%d\nLikes:\t%s", communities, users, posts, likes);
		
		logger.info(result);
		return Response.ok(result).build();
	}

	@DELETE
	@Path("/lru")
	public Response debugLRU(){
		logger.info("Clearing Hyperlogs ...");

		long time = (System.currentTimeMillis() / 1000) - (24 * 60 * 60);

		List<String> dirty_keys;
		dirty_keys = Redis.LRUSetGetDirty(Config.DAYLY_LIKES);
		Redis.del(Config.DAYLY_LIKES);
		for(String post_id : dirty_keys) {
			String query = "SELECT * FROM %s l WHERE l.post_id='" + post_id + "' AND l._ts>=" + time;
			Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(Config.LIKES_CONTAINER, query);
			while(it.hasNext()) {
				List<String> likes = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Like.class)).map(l -> l.getId()).collect(Collectors.toList());
				Redis.LRUHyperLogUpdate(Config.DAYLY_LIKES, post_id, likes, false);
			}
		}

		return Response.ok().build();
	}

}

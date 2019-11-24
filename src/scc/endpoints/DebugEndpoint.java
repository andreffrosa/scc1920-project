package scc.endpoints;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scc.models.Like;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.Config;
import scc.utils.GSON;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Path(DebugEndpoint.PATH)
public class DebugEndpoint {

	public static final String PATH = "/debug";

	public static final String VERSION = "97.0.0-r2 alfa-snapshot-0.0.0.0.0.1 SilkyX-Vanilla Edition";

	static Logger logger = LoggerFactory.getLogger(DebugEndpoint.class);

	@GET
	@Path("/version")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVersion(){
		logger.info(VERSION);
		return Response.ok(VERSION).build();
	}

	@DELETE
	@Path("/container/{name}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("name") String name, String query, @PathParam("key") String key){

		int deleted = CosmosClient.delete(name, query, key);

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

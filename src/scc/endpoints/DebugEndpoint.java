package scc.endpoints;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scc.storage.CosmosClient;
import scc.storage.Redis;

@Path(DebugEndpoint.PATH)
public class DebugEndpoint {

	public static final String PATH = "/debug";

	public static final String VERSION = "93.0.0-r2 alfa-snapshot-0.0.0.0.0.1 SilkyX-Vanilla Edition";

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

}

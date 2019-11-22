package scc.endpoints;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedResponse;

import scc.models.Like;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.storage.SearchClient;
import scc.utils.Config;
import scc.utils.Config.PropType;
import scc.utils.GSON;

@Path(DebugEndpoint.PATH)
public class DebugEndpoint {

	public static final String PATH = "/debug";

	public static final String VERSION = "92.0.0-r2 alfa-snapshot-0.0.0.0.0.1 SilkyX-Vanilla Edition";

	static Logger logger = LoggerFactory.getLogger(DebugEndpoint.class);

	@GET
	@Path("/version")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVersion(){
		logger.info(VERSION);
		return Response.ok(VERSION).build();
	}

	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response search(String search_settings_json){
		JsonObject search_settings = GSON.fromJson(search_settings_json, JsonObject.class);

		JsonObject results = SearchClient.search(search_settings);
		return Response.ok(GSON.toJson(results)).build();
	}

	@DELETE
	@Path("/container/{name}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("name") String name, String query, @PathParam("key") String key){

		int deleted = CosmosClient.delete(name, query, key);

		logger.info("Deleted: " + deleted);
		return Response.ok("Deleted: " + deleted).build();
	}

	@GET
	@Path("/cache")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCache() {
		//		String cache = GSON.toJson(Redis.getMatchingKeys("*"));
		//		return Response.ok(cache).build();

		//Boolean isNull = Config.getRedisProperty(Config.TOP_USERS_LIMIT) == null;

		try {
			Config.getProperties(PropType.REDIS);
			return Response.ok(Config.getProperties(PropType.REDIS) == null).build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.ok(e).build();
		}


	}

	@GET
	@Path("/likes/{post_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLikes(@PathParam("post_id") String post_id) {
		String total_likes = getTotalLikes(post_id);
		return Response.ok("Likes: " + total_likes).build();
	}

	public String getTotalLikes(String post_id) {
		String result = "";
		Long total_likes = Redis.LRUHyperLogGet(Config.TOTAL_LIKES, post_id);
		if(total_likes == null) {
			result += "1";
			if(Redis.ACTIVE) {	
				result += "2";
				String query = "SELECT * FROM %s l WHERE l.post_id='" + post_id + "'";
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(Config.LIKES_CONTAINER, query);
				if(it.hasNext() ) {
					List<Like> likes = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), Like.class)).collect(Collectors.toList());
					Redis.LRUHyperLogPut(Config.TOTAL_LIKES, Integer.parseInt(Config.getRedisProperty(Config.TOTAL_LIKES_LIMIT)), post_id, likes.stream().map(l -> GSON.toJson(l)).collect(Collectors.toList()));
					total_likes = (long) likes.size();
					result += "3";
				} else
					total_likes = 0L;
				while( it.hasNext() ) {
					result += "4";
					List<String> likes = it.next().getResults().stream().map((Document d) -> GSON.toJson(GSON.fromJson(d.toJson(), Like.class))).collect(Collectors.toList());
					Redis.LRUHyperLogUpdate(Config.TOTAL_LIKES, post_id, likes, false);

					total_likes += likes.size();
				}
			} else {
				result += "5";
				String query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='" + post_id + "'";
				List<String> likes = CosmosClient.query(Config.LIKES_CONTAINER, query);
				if (!likes.isEmpty()) {
					result += "6";
					JsonElement root = JsonParser.parseString(likes.get(0));
					total_likes = root.getAsJsonObject().get("Likes").getAsLong();
				} else
					total_likes = 0L;
			}
		} else
			result += total_likes;

		return result;
	}

	//	@GET
	//	@Path("/test")
	//	@Produces(MediaType.APPLICATION_JSON)
	//	public Response test(){
	//		// SELECT VALUE COUNT(1) FROM l WHERE l.post_id='1'
	//		String query_likes = "SELECT VALUE COUNT(1) AS likes FROM l WHERE l.post_id='" + "1" +"'";
	//		List<String> likes = CosmosClient.query(PostResource.LIKES_CONTAINER, query_likes); 
	//		
	//		/*int n_likes = 0;
	//		if(!likes.isEmpty()) {
	//			JsonElement root = new JsonParser().parse(likes.get(0));
	//			n_likes = root.getAsJsonObject().get("Likes").getAsInt();
	//		}*/
	//		
	//		return Response.ok(likes.get(0)).build();
	//	}
}

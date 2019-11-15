package scc.controllers;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import scc.storage.CosmosClient;

@Path(Debug.PATH)
public class Debug {

	public static final String PATH = "/debug" ;

	public static final String VERSION = "45.0.0-r2 alfa-snapshot-0.0.0.0.0.1 SilkyX-Vanilla Edition";

	@GET
	@Path("/version")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVersion(){
		return Response.ok(VERSION).build();
	}

	@GET
	@Path("/test")
	@Produces(MediaType.APPLICATION_JSON)
	public Response test(){
		// SELECT VALUE COUNT(1) FROM l WHERE l.post_id='1'
		String query_likes = "SELECT VALUE COUNT(1) AS likes FROM l WHERE l.post_id='" + "1" +"'";
		List<String> likes = CosmosClient.query(PostResource.LIKE_CONTAINER, query_likes); 
		
		/*int n_likes = 0;
		if(!likes.isEmpty()) {
			JsonElement root = new JsonParser().parse(likes.get(0));
			n_likes = root.getAsJsonObject().get("Likes").getAsInt();
		}*/
		
		return Response.ok(likes.get(0)).build();
	}
}

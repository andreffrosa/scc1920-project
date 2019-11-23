package scc.endpoints;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import scc.models.PostWithReplies;
import scc.resources.PageResource;
import scc.storage.SearchClient;
import scc.utils.Config;
import scc.utils.GSON;

@Path(PageEndpoint.PATH)
public class PageEndpoint {

	public static final String PATH = "/page";
	
	static Logger logger = LoggerFactory.getLogger(PageEndpoint.class);

	@GET
	@Path("/thread/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getThread(@PathParam("id") String id, @QueryParam("d") Integer depth, @QueryParam("ps") Integer page_size, @QueryParam("t") String continuation_token) {

		if(depth == null)
			depth = Integer.parseInt(Config.getSystemProperty(Config.DEFAULT_DEPTH));
		
		if(page_size == null)
			page_size = Integer.parseInt(Config.getSystemProperty(Config.DEFAULT_DEPTH));
		
		PostWithReplies post = PageResource.getThread(id, depth, page_size, continuation_token);

		return GSON.toJson(post);
	}

	@GET
	@Path("/initial")
	@Produces(MediaType.APPLICATION_JSON)
	public String getInitialPage(@QueryParam("ps") Integer page_size, @QueryParam("p") Integer page_number) {

		if(page_size == null)
			page_size = Integer.parseInt(Config.getSystemProperty(Config.DEFAULT_INITIAL_PAGE_SIZE));
		
		if(page_number == null)
			page_number = Integer.parseInt(Config.getSystemProperty(Config.DEFAULT_INITIAL_PAGE_NUMBER));

		logger.info("Page Size: " + page_size + ";\nPage Number: " + page_number +";\n");

		List<PostWithReplies> requested_page = PageResource.getInitialPage(page_size, page_number);
		return GSON.toJson(requested_page);
	}
	
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response search(String search_settings_json) {
		JsonObject search_settings = GSON.fromJson(search_settings_json, JsonObject.class);

		JsonObject results = SearchClient.search(search_settings);
		return Response.ok(GSON.toJson(results)).build();
	}
	
}

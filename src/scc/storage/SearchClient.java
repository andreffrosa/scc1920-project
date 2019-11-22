package scc.storage;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import scc.storage.Exceptions.SearchException;
import scc.utils.Config;
import scc.utils.GSON;

public class SearchClient {

	static Logger logger = LoggerFactory.getLogger(SearchClient.class);

	public static String getHostname() {
		return "https://" + Config.getSearchProperty(Config.SEARCH_SERVICE_NAME) + ".search.windows.net/";
	}

	public static List<JsonElement> search(JsonObject search_settings) throws SearchException {
		String hostname = getHostname();

		Client client = new ResteasyClientBuilder().build();
		URI baseURI = UriBuilder.fromUri(hostname).build();
		WebTarget target = client.target(baseURI);

		String index = Config.SEARCH_INDEX;
		String queryKey = Config.getSearchProperty(Config.SEARCH_QUERY_KEY);

		if(!search_settings.has("count"))
			search_settings.addProperty("count", "true");

		String resultStr = target.path("indexes/" + index + "/docs/search").queryParam("api-version", "2019-05-06")
				.request().header("api-key", queryKey)
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(search_settings.toString(), MediaType.APPLICATION_JSON))
				.readEntity(String.class);

		JsonObject resultObj = GSON.fromJson(resultStr, JsonObject.class);

		if(resultObj.has("error")) {
			logger.info(resultStr);
			throw new SearchException(resultStr);
		} else {
			int number_of_results = resultObj.get("@odata.count") == null ? -1 : resultObj.get("@odata.count").getAsInt();
			List<JsonElement> result = new ArrayList<JsonElement>(number_of_results > 0 ? number_of_results : 1);

			logger.info("Number of results : " + number_of_results);

			for( JsonElement el: resultObj.get("value").getAsJsonArray()) {
				JsonObject elObj = el.getAsJsonObject();
				
				if(search_settings.has("select")) {
					String[] members = GSON.fromJson(search_settings.get("select").toString(), String.class).split(",");
					
					JsonObject selected = new JsonObject();
					for(int i = 0; i < members.length; i++) {
						selected.add(members[i], elObj.get(members[i]));
					}
					result.add(selected);
				} else {
					//elObj.remove("@search.score");
					result.add(elObj);
				}
			}
			
			return result;
		}
	}

}

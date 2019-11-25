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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import scc.utils.Config;
import scc.utils.GSON;

public class SearchClient {

	static Logger logger = LoggerFactory.getLogger(SearchClient.class);

	public static String getHostname() {
		return "https://" + Config.getSearchProperty(Config.SEARCH_SERVICE_NAME) + ".search.windows.net/";
	}

	public static JsonObject search(JsonObject search_settings) {
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
			return resultObj;
		} else {
			int number_of_results = resultObj.get("@odata.count") == null ? -1 : resultObj.get("@odata.count").getAsInt();

			JsonObject result = new JsonObject();

			logger.info("Number of results : " + number_of_results);

			List<JsonElement> results = new ArrayList<JsonElement>(number_of_results > 0 ? number_of_results : 1);

			for( JsonElement el: resultObj.get("value").getAsJsonArray()) {
				JsonObject elObj = el.getAsJsonObject();

				if(search_settings.has("select")) {
					String[] members = GSON.fromJson(search_settings.get("select").toString(), String.class).split(",");

					JsonObject selected = new JsonObject();
					for(int i = 0; i < members.length; i++) {
						selected.add(members[i], elObj.get(members[i]));
					}
					results.add(selected);

				} else {
					elObj.remove("@search.score");
					results.add(elObj);
				}
			}
			result.addProperty("total_results", number_of_results);
			result.add("results", GSON.fromJson(GSON.toJson(results), JsonArray.class));
			return result;
		}
	}

}

package scc.controllers;

import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.cosmosdb.ConnectionMode;
import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedOptions;
import com.microsoft.azure.cosmosdb.FeedResponse;
import com.microsoft.azure.cosmosdb.ResourceResponse;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;

import rx.Observable;
import scc.models.Community;

@Path("/community")
public class CommunityResource {

	private static final String COSMOS_DB_ENDPOINT = "https://cosmos-48043.documents.azure.com:443/";
	private static final String COSMOS_DB_MASTER_KEY = "cmqCzSEYRX5E2GLF2kxF3ftlEXZnZLLCRA4nZvb4jpH5gLZN6oWiPtGpLWx2l2iRvQ0IHwA8DmKPq33KqdNwog==";
	private static final String COSMOS_DB_DATABASE = "cosmos-48043";
	
	private static final String CommunitiesCollection = UriFactory.CreateDocumentCollectionUri(COSMOS_DB_DATABASE, "Communities");//"/dbs/cosmos-48043/colls/Communities/";

	private AsyncDocumentClient cosmos_client;

	public CommunityResource() throws Exception {
		//ConnectionPolicy connectionPolicy = new ConnectionPolicy(); //ConnectionPolitcy.getDefault()
		ConnectionPolicy connectionPolicy = ConnectionPolicy.GetDefault();
		//connectionPolicy.setConnectionMode(ConnectionMode.Direct);
		cosmos_client = new AsyncDocumentClient.Builder()
				.withServiceEndpoint(COSMOS_DB_ENDPOINT)
				.withMasterKeyOrResourceToken(COSMOS_DB_MASTER_KEY)
				.withConnectionPolicy(connectionPolicy)
				.withConsistencyLevel(ConsistencyLevel.Eventual)
				.build();
	}

	// TODO: Fazer as excepções como deve ser
	// TODO: Fazer as replys como o dred tem nos slides
	
	@POST
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createCommunity(@PathParam("name") String name) {
		try {
			Observable<ResourceResponse<Document>> resp = 
					cosmos_client.createDocument(CommunitiesCollection, new Community(name), null, false);
			
			return Response.ok(resp.toBlocking().first().getResource().getId(), MediaType.APPLICATION_JSON).build();
		} catch(Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}
	}

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response consultCommunity(@PathParam("name") String name) {
		try {
			FeedOptions queryOptions = new FeedOptions();
			queryOptions.setEnableCrossPartitionQuery(true);
			queryOptions.setMaxDegreeOfParallelism(-1);
			Iterator<FeedResponse<Document>> it = 
					cosmos_client.queryDocuments(CommunitiesCollection,
			"SELECT * FROM Communities c WHERE c.name = '" + name + "'",
			queryOptions).toBlocking().getIterator();
			
			// NOTE: multiple documents can be returned or none
			if (it.hasNext()) {
				String doc = it.next().getResults().get(0).toJson();
				return Response.ok(doc, MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		} catch(Exception e) {
			return Response.serverError().entity(e).build();
		}
	}

}

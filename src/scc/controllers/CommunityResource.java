package scc.controllers;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.azure.cosmosdb.*;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import rx.Observable;
import scc.models.Community;

import javax.ws.rs.*;
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
import com.microsoft.azure.cosmosdb.internal.directconnectivity.ConflictException;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;

import rx.Observable;
import rx.functions.Action1;
import scc.models.Community;


@Path("/community")
public class CommunityResource {

	private static final String COSMOS_DB_ENDPOINT = "https://cloud-1920.documents.azure.com:443/";
	private static final String COSMOS_DB_MASTER_KEY ="d2uk6OuA3b8jzqXBIK2yhgw9VVKMBhxpp3zXUi5uG2v3U6pTI1M2W9wUBjQ1gFIcGOnnlJbRmCSZtWPRchBk6Q=="; // "cmqCzSEYRX5E2GLF2kxF3ftlEXZnZLLCRA4nZvb4jpH5gLZN6oWiPtGpLWx2l2iRvQ0IHwA8DmKPq33KqdNwog==";
	private static final String COSMOS_DB_DATABASE =  "cloud-2019";// "scc1920-48043";
	
	//private static final String CommunitiesCollection = Microsoft.Azure.Documents.Client.UriFactory.CreateDocumentCollectionUri(COSMOS_DB_DATABASE, "Communities");//"/dbs/cosmos-48043/colls/Communities/";
	// private static final String CommunitiesCollection = "/dbs/scc1920-48043/colls/Communities/";
	private static final String CommunitiesCollection = String.format("/dbs/%s/colls/%s", COSMOS_DB_DATABASE, "Communities");

	private AsyncDocumentClient cosmos_client;

	public CommunityResource() throws Exception {
		ConnectionPolicy connectionPolicy = new ConnectionPolicy(); //ConnectionPolitcy.getDefault()
		//ConnectionPolicy connectionPolicy = ConnectionPolicy.GetDefault();
		connectionPolicy.setConnectionMode(ConnectionMode.Direct);
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

		/*try {
			Observable<ResourceResponse<Document>> resp = 
=======
		try {
			Observable<ResourceResponse<Document>> resp =
>>>>>>> 4cde635cd5729ffae7c40c70748096e64dbdf3ff
					cosmos_client.createDocument(CommunitiesCollection, new Community(name), null, false);

			return Response.ok(resp.toBlocking().first().getResource().getId(), MediaType.APPLICATION_JSON).build();
		} catch(Exception e) {

			// TODO: Perguntar como capturar as excepções como deve ser
			if(e.getMessage().contains("statusCode=409"))
				return Response.status(Status.CONFLICT).entity("Community with the specified name already exists in the system.").build();
			
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
<<<<<<< HEAD
		}*/
		
        /*Observable<ResourceResponse<Document>> createDocumentObservable = cosmos_client
                .createDocument(CommunitiesCollection, new Community(name), null, false);

        final CountDownLatch completionLatch = new CountDownLatch(1);
        
        AtomicReference<String> at = new AtomicReference<>();

        // Subscribe to Document resource response emitted by the observable
        createDocumentObservable.single() // We know there will be one response
        	//.doOnError(throwable -> at2.set(throwable.getClass().getName()))
        	.subscribe(documentResourceResponse -> {
                    System.out.println(documentResourceResponse.getActivityId());
                    at.set(documentResourceResponse.getResource().getId());
                    completionLatch.countDown();
                }, error -> {
                    System.err.println(
                    */ //       "an error occurred while creating the document: actual cause: " + /*error.getMessage()*/ error.getClass().getName());
                    /*at.set("xuxuxux " + error.getClass().getName());
                    completionLatch.countDown();
                });

        // Wait till document creation completes
        try {
			completionLatch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
=======

>>>>>>> 4cde635cd5729ffae7c40c70748096e64dbdf3ff
		}
        
        
		
        return Response.ok(at.get(), MediaType.APPLICATION_JSON).build();*/
		
		return AsyncCreate(CommunitiesCollection, new Community(name), 
				response -> {
					return Response.ok(response.getResource().getId(), MediaType.APPLICATION_JSON).build();
				}, error -> {
					if(error instanceof ConflictException)
						return Response.status(Status.CONFLICT).entity("Community with the specified name already exists in the system.").build();
					
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(error.getMessage()).build();
				});
	}

	@FunctionalInterface
	public interface Foo {
	    Response method(ResourceResponse<Document> response);
	}
	
	@FunctionalInterface
	public interface Goo {
	    Response method(Throwable error);
	}
	
	private Response AsyncCreate(String collectionLink, Object o, Foo onResponse, Goo onError) {
		 Observable<ResourceResponse<Document>> createDocumentObservable = cosmos_client
	                .createDocument(collectionLink, o, null, false);

	        final CountDownLatch completionLatch = new CountDownLatch(1);
	        
	        AtomicReference<Response> at = new AtomicReference<>();

	        // Subscribe to Document resource response emitted by the observable
	        createDocumentObservable.single() // We know there will be one response
	        	//.doOnError(throwable -> at2.set(throwable.getClass().getName()))
	        	.subscribe(documentResourceResponse -> {
	                    at.set(onResponse.method(documentResourceResponse));
	                    completionLatch.countDown();
	                }, error -> {
	                   // System.err.println(
	                    //        "an error occurred while creating the document: actual cause: " + /*error.getMessage()*/ error.getClass().getName());
	                    //at.set("xuxuxux " + error.getClass().getName());
	                    at.set(onError.method(error));
	                    completionLatch.countDown();
	                });

	        // Wait till document creation completes
	        try {
				completionLatch.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	        return at.get();
	}
	
	
	//TODO: GET DE COISA QUE NÂO EXISTE LEVA INFINITO TEMPO E RETORNA There was an unexpected error in the request processing.
	
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
				return Response.status(Status.NOT_FOUND).entity("No Community found with the specified name in the system.").build();
			}
		} catch(Exception e) {
			return Response.serverError().entity(e).build();
		}
	}

}

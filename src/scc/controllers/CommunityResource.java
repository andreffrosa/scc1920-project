package scc.controllers;


import com.microsoft.azure.cosmosdb.User;

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
public class CommunityResource extends Resource {

	public CommunityResource() throws Exception {
	    super(Community.DataType);
	}

	// TODO: Fazer as excepções como deve ser
	// TODO: Fazer as replys como o dred tem nos slides
	
	@POST
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createCommunity(@PathParam("name") String name) {

		return null;

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
		return super.findByName(name);
	}


	@PUT
	@Path("/{name}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(Community c){
		return super.update(c);
	}

	@DELETE
	@Path("/{name}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("name") String name){
		return super.delete(name);
	}

}


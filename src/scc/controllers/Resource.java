package scc.controllers;

import com.microsoft.azure.cosmosdb.*;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Resource {

	private static final String COSMOS_DB_ENDPOINT = "https://cloud-1920.documents.azure.com:443/";
	private static final String COSMOS_DB_MASTER_KEY = "d2uk6OuA3b8jzqXBIK2yhgw9VVKMBhxpp3zXUi5uG2v3U6pTI1M2W9wUBjQ1gFIcGOnnlJbRmCSZtWPRchBk6Q=="; // "cmqCzSEYRX5E2GLF2kxF3ftlEXZnZLLCRA4nZvb4jpH5gLZN6oWiPtGpLWx2l2iRvQ0IHwA8DmKPq33KqdNwog==";
	private static final String COSMOS_DB_DATABASE = "cloud-2019";// "scc1920-48043";

	private String collection;
	private String collectionLink;
	private AsyncDocumentClient cosmos_client;

	Resource(String collection) {
		this.collection = collection;
		collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_DATABASE, collection);
		ConnectionPolicy connectionPolicy = new ConnectionPolicy();
		connectionPolicy.setConnectionMode(ConnectionMode.Direct);
		cosmos_client = new AsyncDocumentClient.Builder().withServiceEndpoint(COSMOS_DB_ENDPOINT)
				.withMasterKeyOrResourceToken(COSMOS_DB_MASTER_KEY).withConnectionPolicy(connectionPolicy)
				.withConsistencyLevel(ConsistencyLevel.Eventual).build();
	}

	@FunctionalInterface
	public interface ResponseHandler {
		Response execute(ResourceResponse<Document> response);
	}

	@FunctionalInterface
	public interface ErrorHandler {
		Response execute(Throwable error);
	}

	protected Response create(Object o, ResponseHandler onResponse, ErrorHandler onError) {
		Observable<ResourceResponse<Document>> createDocumentObservable = cosmos_client.createDocument(collectionLink,
				o, null, false);

		final CountDownLatch completionLatch = new CountDownLatch(1);

		AtomicReference<Response> at = new AtomicReference<>();

		// Subscribe to Document resource response emitted by the observable
		createDocumentObservable.single() // We know there will be one response
				.subscribe(documentResourceResponse -> {
					at.set(onResponse.execute(documentResourceResponse));
					completionLatch.countDown();
				}, error -> {
					at.set(onError.execute(error));
					completionLatch.countDown();
				});

		// Wait till document creation completes
		try {
			completionLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return at.get();
	}

	/*
	 * public Response create(Object o){ Response create(Object o){ try {
	 * Observable<ResourceResponse<Document>> resp =
	 * cosmos_client.createDocument(collectionLink, o, null, false);
	 * 
	 * return Response.ok(resp.toBlocking().first().getResource().getId(),
	 * MediaType.APPLICATION_JSON).build(); } catch(Exception e) { return
	 * Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getStackTrace
	 * ()).build(); }
	 * 
	 * }
	 */

	public Response findByName(String name) {

		try {
			FeedOptions queryOptions = new FeedOptions();
			queryOptions.setEnableCrossPartitionQuery(true);
			queryOptions.setMaxDegreeOfParallelism(-1);
			Iterator<FeedResponse<Document>> it = cosmos_client.queryDocuments(collectionLink,
					"SELECT * FROM " + collection + " c WHERE c.name = '" + name + "'", queryOptions).toBlocking()
					.getIterator();

			// NOTE: multiple documents can be returned or none
			if (it.hasNext()) {
				String doc = it.next().getResults().get(0).toJson();
				return Response.ok(doc, MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
		} catch (Exception e) {
			return Response.serverError().entity(e).build();
		}

	}

	/*Response update(Object o) {
		return null;
	}

	public Response delete(String name) {

		String documentLink = String.format("%s/docs/%s", collectionLink, name);
		Observable<ResourceResponse<Document>> createDocumentObservable = cosmos_client.deleteDocument(documentLink,
				null);
		createDocumentObservable.single() // we know there will be one response
				.subscribe(documentResourceResponse -> {
					System.out.println(documentResourceResponse.getResource().getId());

				}, error -> {

					System.err.println("an error happened: " + error.getMessage());
				});
		return null;
	}*/

}

package scc.storage;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.Response;

import com.microsoft.azure.cosmosdb.ConnectionMode;
import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.DocumentClientException;
import com.microsoft.azure.cosmosdb.FeedOptions;
import com.microsoft.azure.cosmosdb.FeedResponse;
import com.microsoft.azure.cosmosdb.ResourceResponse;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;

import rx.Observable;

public class CosmosClient {

	private static AsyncDocumentClient cosmosClient;
	private static String cosmosDatabase;

	private CosmosClient() {}
	
	public static AsyncDocumentClient getCosmosClient(){
		return cosmosClient;
	}

	public static String getCosmosDatabase(){
		return cosmosDatabase;
	}

	public static void init(String cosmosDB, String cosmosDbMasterKey, String cosmosDbEndpoint) {
		cosmosDatabase = cosmosDB;

		ConnectionPolicy connectionPolicy = new ConnectionPolicy();
		connectionPolicy.setConnectionMode(ConnectionMode.Direct);
		cosmosClient = new AsyncDocumentClient.Builder().withServiceEndpoint(cosmosDbEndpoint)
				.withMasterKeyOrResourceToken(cosmosDbMasterKey).withConnectionPolicy(connectionPolicy)
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

	public static String create(String container_name, Object o) throws DocumentClientException  {

		String collectionLink = String.format("/dbs/%s/colls/%s", cosmosDatabase, container_name);

		Observable<ResourceResponse<Document>> createDocumentObservable = cosmosClient.createDocument(collectionLink, o, null, false);

		final CountDownLatch completionLatch = new CountDownLatch(1);

		AtomicReference<String> at = new AtomicReference<>();
		AtomicReference<DocumentClientException> at2 = new AtomicReference<>();

		// Subscribe to Document resource response emitted by the observable
		createDocumentObservable.single() // We know there will be one response
		.subscribe(documentResourceResponse -> {
			at.set(documentResourceResponse.getResource().getId());
			completionLatch.countDown();
		}, error -> {			
			if(error instanceof DocumentClientException) {
				at2.set(((DocumentClientException)error));
			}
			at2.set(new DocumentClientException(500, error.getMessage()));
			completionLatch.countDown();
		});

		// Wait till document creation completes
		try {
			completionLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		DocumentClientException e = at2.get();
		if(e == null) {
			return at.get();
		} else {
			throw e;
		}
	}

	public static <T> String getByName(String container_name, String name) {
		
		String collectionLink = String.format("/dbs/%s/colls/%s", cosmosDatabase, container_name);
		
		//try {
			FeedOptions queryOptions = new FeedOptions();
			queryOptions.setEnableCrossPartitionQuery(true);
			queryOptions.setMaxDegreeOfParallelism(-1);
			Iterator<FeedResponse<Document>> it = cosmosClient.queryDocuments(collectionLink,
					"SELECT * FROM " + container_name + " c WHERE c.name = '" + name + "'", queryOptions).toBlocking()
					.getIterator();

			// NOTE: multiple documents can be returned or none
			if (it.hasNext()) {
				String doc = it.next().getResults().get(0).toJson();
				return doc;
			} else {
				return null;
			}
		/*} catch (Exception e) {
			//return Response.serverError().entity(e).build();
			return null; //TODO: fazer isto como deve ser
		}*/
	}

	public static String getById(String container_name, String id) {
		String collectionLink = String.format("/dbs/%s/colls/%s", cosmosDatabase, container_name);

		//try {
		FeedOptions queryOptions = new FeedOptions();
		queryOptions.setEnableCrossPartitionQuery(true);
		queryOptions.setMaxDegreeOfParallelism(-1);
		Iterator<FeedResponse<Document>> it = cosmosClient.queryDocuments(collectionLink,
				"SELECT * FROM " + container_name + " c WHERE c.id = '" + id + "'", queryOptions).toBlocking()
				.getIterator();

		// NOTE: multiple documents can be returned or none
		if (it.hasNext()) {
			String doc = it.next().getResults().get(0).toJson();
			return doc;
		} else {
			return null;
		}
		/*} catch (Exception e) {
			//return Response.serverError().entity(e).build();
			return null; //TODO: fazer isto como deve ser
		}*/
	}

}

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

public class CosmosClientSingleton {

	private static AsyncDocumentClient cosmosClient;
	private static String cosmosDatabase;

	public AsyncDocumentClient getCosmosClient(){
		return cosmosClient;
	}

	public String getCosmosDatabase(){
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

	public static String create(String container_name, Object o)  {

		String collectionLink = String.format("/dbs/%s/colls/%s", cosmosDatabase, container_name);

		Observable<ResourceResponse<Document>> createDocumentObservable = cosmosClient.createDocument(collectionLink, o, null, false);

		final CountDownLatch completionLatch = new CountDownLatch(1);

		AtomicReference<String> at = new AtomicReference<>();
		AtomicReference<Integer> at2 = new AtomicReference<>();

		// Subscribe to Document resource response emitted by the observable
		createDocumentObservable.single() // We know there will be one response
		.subscribe(documentResourceResponse -> {
			at.set(documentResourceResponse.getResource().getId());
			at2.set(200);
			completionLatch.countDown();
		}, error -> {			
			if(error instanceof DocumentClientException) {
				at2.set(((DocumentClientException)error).getStatusCode());
			} else
				at2.set(500);

			at.set(error.getMessage());
			completionLatch.countDown();
		});

		// Wait till document creation completes
		try {
			completionLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		int status_code = at2.get();
		if(status_code == 200) {
			return at.get();
		} else {
			// TODO: Lançar excepções
			return null;
		}
	}

	public <T> String getByName(String container_name, String name) {
		
		String collectionLink = String.format("/dbs/%s/colls/%s", cosmosDatabase, container_name);
		
		try {
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
		} catch (Exception e) {
			//return Response.serverError().entity(e).build();
			return null; //TODO: fazer isto como deve ser
		}
	}

}

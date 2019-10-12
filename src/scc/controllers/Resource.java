package scc.controllers;

import com.microsoft.azure.cosmosdb.*;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import rx.Observable;
import scc.config.Exceptions.CosmosDatabaseIdNotFound;
import scc.config.Exceptions.EndpointURLNotFound;
import scc.config.Exceptions.MasterKeyNotFound;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static scc.config.Config.loadConfig;

public class Resource {

	private static final String COSMOS_CONFIG_FILE_PATH = "config/Cosmos.conf";

	private static final String COSMOS_DB_ENDPOINT = "cosmos_db_endpoint";
	private static final String COSMOS_DB_MASTER_KEY = "cosmos_db_master_key";
	private static final String COSMOS_DB_DATABASE = "cosmos_db_database";

	private String collection;
	private String collectionLink;
	private AsyncDocumentClient cosmos_client;

	Resource(String collection) throws IOException, CosmosDatabaseIdNotFound, MasterKeyNotFound, EndpointURLNotFound {
		this.collection = collection;

		Properties props = loadConfig(COSMOS_CONFIG_FILE_PATH);
		if(props.contains(COSMOS_DB_DATABASE))
			collectionLink = String.format("/dbs/%s/colls/%s", props.getProperty(COSMOS_DB_DATABASE), collection);
		else
			throw new CosmosDatabaseIdNotFound();

		buildClient(props);
	}

	private void buildClient(Properties props) throws MasterKeyNotFound, EndpointURLNotFound {

		if (!props.contains(COSMOS_DB_MASTER_KEY)) throw new MasterKeyNotFound();

		if (!props.contains(COSMOS_DB_ENDPOINT)) throw new EndpointURLNotFound();

		ConnectionPolicy connectionPolicy = new ConnectionPolicy();
		connectionPolicy.setConnectionMode(ConnectionMode.Direct);
		cosmos_client = new AsyncDocumentClient.Builder().withServiceEndpoint(props.getProperty(COSMOS_DB_ENDPOINT))
				.withMasterKeyOrResourceToken(props.getProperty(COSMOS_DB_MASTER_KEY)).withConnectionPolicy(connectionPolicy)
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

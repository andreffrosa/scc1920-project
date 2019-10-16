package scc.controllers;

import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedOptions;
import com.microsoft.azure.cosmosdb.FeedResponse;
import com.microsoft.azure.cosmosdb.ResourceResponse;
import rx.Observable;
import scc.storage.Config;
import scc.storage.CosmosClientSingleton;
import scc.storage.Exceptions.CosmosDatabaseIdNotFound;
import scc.storage.Exceptions.EndpointURLNotFound;
import scc.storage.Exceptions.MasterKeyNotFound;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;


public class Resource {

	private String collection;
	private String collectionLink;
	private CosmosClientSingleton cosmosClientSingleton;

	Resource(String collection) throws IOException, CosmosDatabaseIdNotFound, MasterKeyNotFound, EndpointURLNotFound {

		cosmosClientSingleton = Config.getCosmosDBClientInstance();
		this.collection = collection;
		collectionLink = String.format("/dbs/%s/colls/%s", cosmosClientSingleton.getCosmosDatabase(), collection);
	}

	

	public Response findByName(String name) {

		try {
			FeedOptions queryOptions = new FeedOptions();
			queryOptions.setEnableCrossPartitionQuery(true);
			queryOptions.setMaxDegreeOfParallelism(-1);
			Iterator<FeedResponse<Document>> it = cosmosClientSingleton.getCosmosClient().queryDocuments(collectionLink,
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

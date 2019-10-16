package scc.controllers;

import java.io.IOException;

import com.microsoft.azure.cosmosdb.DocumentClientException;

import scc.storage.CosmosClient;
import scc.storage.Exceptions.CosmosDatabaseIdNotFound;
import scc.storage.Exceptions.EndpointURLNotFound;
import scc.storage.Exceptions.MasterKeyNotFound;

public class Resource {

	private String collection;

	Resource(String collection) throws IOException, CosmosDatabaseIdNotFound, MasterKeyNotFound, EndpointURLNotFound {
		this.collection = collection;
	}

	public String create(Object o) throws DocumentClientException {
		return CosmosClient.create(collection, o);
	}
	
	public String getByName(String name) {
		return CosmosClient.getByName(collection, name);
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

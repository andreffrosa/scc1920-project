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

	public String getById(String id) { return CosmosClient.getById(collection, id); }


	/*Response update(Object o) {
		return null;
	}
	*/

	public void delete(String id) throws DocumentClientException {
		CosmosClient.delete(collection, id);
	}

}

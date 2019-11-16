package scc.controllers;

import java.io.IOException;

import scc.storage.Exceptions.CosmosDatabaseIdNotFound;
import scc.storage.Exceptions.EndpointURLNotFound;
import scc.storage.Exceptions.MasterKeyNotFound;

public class Resource {

	protected String collection;

	Resource(String collection) throws IOException, CosmosDatabaseIdNotFound, MasterKeyNotFound, EndpointURLNotFound {
		this.collection = collection;
	}

}

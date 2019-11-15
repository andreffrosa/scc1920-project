package scc.storage;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import com.microsoft.azure.cosmosdb.ConnectionMode;
import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.DocumentClientException;
import com.microsoft.azure.cosmosdb.FeedOptions;
import com.microsoft.azure.cosmosdb.FeedResponse;
import com.microsoft.azure.cosmosdb.ResourceResponse;
import com.microsoft.azure.cosmosdb.internal.directconnectivity.ConflictException;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;

import rx.Observable;
import scc.utils.GSON;

public class CosmosClient {

	private static AsyncDocumentClient cosmosClient;
	private static String cosmosDatabase;

	private CosmosClient() {
	}

	public static AsyncDocumentClient getCosmosClient() {
		return cosmosClient;
	}

	public static String getCosmosDatabase() {
		return cosmosDatabase;
	}

	public static void init(String cosmosDB, String cosmosDbMasterKey, String cosmosDbEndpoint) {
		cosmosDatabase = cosmosDB;

		ConnectionPolicy connectionPolicy = new ConnectionPolicy();
		connectionPolicy.setConnectionMode(ConnectionMode.Direct);
		cosmosClient = new AsyncDocumentClient.Builder().withServiceEndpoint(cosmosDbEndpoint)
				.withMasterKeyOrResourceToken(cosmosDbMasterKey).withConnectionPolicy(connectionPolicy)
				.withConsistencyLevel(ConsistencyLevel.Session).build();
	}

	@FunctionalInterface
	public interface ResponseHandler {
		Response execute(ResourceResponse<Document> response);
	}

	@FunctionalInterface
	public interface ErrorHandler {
		Response execute(Throwable error);
	}

	public static String getColllectionLink(String dabase_name, String container_name) {
		return String.format("/dbs/%s/colls/%s", dabase_name, container_name);
	}
	
	public static String getDocumentLink(String dabase_name, String container_name, String id) {
		return String.format("/dbs/%s/colls/%s/docs/%s", dabase_name, container_name, id);
	}
	
	
	public static String create(String container_name, Object o) throws DocumentClientException {

		String collectionLink = getColllectionLink(cosmosDatabase, container_name);

		Observable<ResourceResponse<Document>> createDocumentObservable = cosmosClient.createDocument(collectionLink, o,
				null, false);

		final CountDownLatch completionLatch = new CountDownLatch(1);

		AtomicReference<String> at = new AtomicReference<>();
		AtomicReference<DocumentClientException> at2 = new AtomicReference<>();

		// Subscribe to Document resource response emitted by the observable
		createDocumentObservable.single() // We know there will be one response
				.subscribe(documentResourceResponse -> {
					at.set(documentResourceResponse.getResource().getId());
					completionLatch.countDown();
				}, error -> {
					if (error instanceof ConflictException) {
						at2.set(new DocumentClientException(Response.Status.CONFLICT.getStatusCode(),
								(Exception) error));
					} else {
						at2.set(new DocumentClientException(500, (Exception) error));
					}
					error.printStackTrace();
					
					completionLatch.countDown();
				});

		// Wait till document creation completes
		try {
			completionLatch.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		DocumentClientException e = at2.get();
		if (e == null) {
			return at.get();
		} else {
			throw e;
		}
	}

	public static void delete(String container_name, String id) throws DocumentClientException {

		String documentLink = getDocumentLink(cosmosDatabase, container_name, id);
		Observable<ResourceResponse<Document>> createDocumentObservable = cosmosClient.deleteDocument(documentLink,
				null);

		final CountDownLatch completionLatch = new CountDownLatch(1);
		AtomicReference<DocumentClientException> at = new AtomicReference<>();

		createDocumentObservable.single() // we know there will be one response
				.subscribe(documentResourceResponse -> {
					completionLatch.countDown();
				}, error -> {
					if (error instanceof DocumentClientException)
						at.set(new DocumentClientException(Response.Status.NOT_FOUND.getStatusCode(),
								(Exception) error));
					else
						at.set(new DocumentClientException(500, (Exception) error));
					
					error.printStackTrace();
					
					completionLatch.countDown();
				});

		// Wait till document creation completes
		try {
			completionLatch.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		DocumentClientException e = at.get();
		if (e != null) {
			throw e;
		}
	}

	public static <T> String getByName(String container_name, String name) {
		String collectionLink = getColllectionLink(cosmosDatabase, container_name);

		// try {
		FeedOptions queryOptions = new FeedOptions();
		queryOptions.setEnableCrossPartitionQuery(true);
		queryOptions.setMaxDegreeOfParallelism(-1);
		Iterator<FeedResponse<Document>> it = cosmosClient.queryDocuments(collectionLink,
				"SELECT * FROM " + container_name + " c WHERE c.name = '" + name + "'", queryOptions).toBlocking()
				.getIterator();

		// NOTE: multiple documents can be returned or none
		if (it.hasNext()) {
			List<Document> doc = it.next().getResults();
			if (doc.size() > 0)
				return doc.get(0).toJson();
		}

		return null;

	}

	public static String getById(String container_name, String id) {
		String collectionLink = getColllectionLink(cosmosDatabase, container_name);

		// try {
		FeedOptions queryOptions = new FeedOptions();
		queryOptions.setEnableCrossPartitionQuery(true);
		queryOptions.setMaxDegreeOfParallelism(-1);
		Iterator<FeedResponse<Document>> it = cosmosClient.queryDocuments(collectionLink,
				"SELECT * FROM " + container_name + " c WHERE c.id = '" + id + "'", queryOptions).toBlocking()
				.getIterator();

		// NOTE: multiple documents can be returned or none
		if (it.hasNext()) {
			List<Document> doc = it.next().getResults();
			if (doc.size() > 0)
				return doc.get(0).toJson();

		}

		return null;
	}

	public static <T> T getByIdUnparse(String container_name, String id, Class<T> class_) {
		String collectionLink = getColllectionLink(cosmosDatabase, container_name);

		// try {
		FeedOptions queryOptions = new FeedOptions();
		queryOptions.setEnableCrossPartitionQuery(true);
		queryOptions.setMaxDegreeOfParallelism(-1);
		Iterator<FeedResponse<Document>> it = cosmosClient.queryDocuments(collectionLink,
				"SELECT * FROM " + container_name + " c WHERE c.id = '" + id + "'", queryOptions).toBlocking()
				.getIterator();

		// NOTE: multiple documents can be returned or none
		if (it.hasNext()) {
			List<Document> doc = it.next().getResults();
			if (doc.size() > 0) {
				return GSON.fromJson(doc.get(0).toJson(), class_);
			}

		}

		return null;
	}

	public static <T> List<String> getNewest(String container_name) {
		String collectionLink = getColllectionLink(cosmosDatabase, container_name);

		// try {
		FeedOptions queryOptions = new FeedOptions();
		queryOptions.setEnableCrossPartitionQuery(true);
		queryOptions.setMaxDegreeOfParallelism(-1);
		Iterator<FeedResponse<Document>> it = cosmosClient.queryDocuments(collectionLink,
				"SELECT * FROM " + container_name + " c ORDER BY c.creationTime DESC", queryOptions).toBlocking()
				.getIterator();

		List<String> list = new LinkedList<String>();

		while (it.hasNext()) {
			List<String> l = it.next().getResults().stream()
					.map(d -> d.toJson()).collect(Collectors.toList());
			list.addAll(l);
		}

		return list;
	}

	public static List<String> query(String container_name, String query) {
		String collectionLink = getColllectionLink(cosmosDatabase, container_name);

		FeedOptions queryOptions = new FeedOptions();
		queryOptions.setEnableCrossPartitionQuery(true);
		queryOptions.setMaxDegreeOfParallelism(-1);
		Iterator<FeedResponse<Document>> it = cosmosClient
				.queryDocuments(collectionLink, String.format(query, container_name), queryOptions).toBlocking()
				.getIterator();

		List<String> list = new LinkedList<String>();

		while (it.hasNext()) {
			List<String> l = it.next().getResults().parallelStream().map(d -> d.toJson()).collect(Collectors.toList());
			list.addAll(l);
		}

		return list;
	}

	public static <T> List<T> queryAndUnparse(String container_name, String query, Class<T> class_) {
		String collectionLink = getColllectionLink(cosmosDatabase, container_name);

		String final_query = String.format(query, container_name);

		FeedOptions queryOptions = new FeedOptions();
		queryOptions.setEnableCrossPartitionQuery(true);
		queryOptions.setMaxDegreeOfParallelism(-1);
		Observable<FeedResponse<Document>> queryObservable = cosmosClient.queryDocuments(collectionLink, final_query,
				queryOptions);

		// Observable to Iterator
		Iterator<FeedResponse<Document>> it = queryObservable.toBlocking().getIterator();

		List<T> list = new LinkedList<T>();
		while (it.hasNext()) {
			List<T> l = it.next().getResults().parallelStream().map(d -> GSON.fromJson(d.toJson(), class_))
					.collect(Collectors.toList());
			list.addAll(l);
		}

		return list;
	}

	public static <T> Entry<String,List<T>> queryAndUnparsePaginated(String container_name, String query, String continuationToken, int pageSize, Class<T> class_) {
		String collectionLink = getColllectionLink(cosmosDatabase, container_name);

		String final_query = String.format(query, container_name);

		FeedOptions queryOptions = new FeedOptions();
		queryOptions.setMaxItemCount(pageSize);
		queryOptions.setEnableCrossPartitionQuery(true);
		queryOptions.setMaxDegreeOfParallelism(-1);
		if (continuationToken != null)
			queryOptions.setRequestContinuation(continuationToken);

		Observable<FeedResponse<Document>> queryObservable = cosmosClient.queryDocuments(collectionLink, final_query, queryOptions);

		// Observable to Iterator
		Iterator<FeedResponse<Document>> it = queryObservable.toBlocking().getIterator();

		List<T> list = new LinkedList<T>();
		if(it.hasNext()) {
			FeedResponse<Document> page = it.next();
			List<T> l = page.getResults().parallelStream().map(d -> GSON.fromJson(d.toJson(), class_))
					.collect(Collectors.toList());
			list.addAll(l);
			
			continuationToken = page.getResponseContinuation();
		}

		return new AbstractMap.SimpleEntry<>(continuationToken, list);
	}
	
	public static <T> Iterator<FeedResponse<Document>> queryIterator(String container_name, String query) {
		String collectionLink = getColllectionLink(cosmosDatabase, container_name);

		String final_query = String.format(query, container_name);

		FeedOptions queryOptions = new FeedOptions();
		queryOptions.setEnableCrossPartitionQuery(true);
		queryOptions.setMaxDegreeOfParallelism(-1);

		Observable<FeedResponse<Document>> queryObservable = cosmosClient.queryDocuments(collectionLink, final_query,
				queryOptions);

		// Observable to Iterator
		Iterator<FeedResponse<Document>> it = queryObservable.toBlocking().getIterator();

		return it;
	}

}

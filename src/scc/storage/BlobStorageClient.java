package scc.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

public class BlobStorageClient {

	private static final int DEFAULT_SIZE = 1;
	private static Map<String, CloudBlobContainer> blobStorageContainers = new HashMap<String, CloudBlobContainer>(DEFAULT_SIZE);
	private static CloudStorageAccount storageAccount;
	private static CloudBlobClient blobClient;

	private BlobStorageClient () {}

	public static void init(String connection_string) {
		try {
			storageAccount = CloudStorageAccount.parse(connection_string);
			blobClient = storageAccount.createCloudBlobClient();
		} catch (InvalidKeyException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static CloudBlobContainer newContainer(String connection_string, String container_name) throws StorageException, InvalidKeyException, URISyntaxException {

		CloudBlobContainer container = blobStorageContainers.get(container_name);
		if (container == null){
			container = blobClient.getContainerReference(container_name);
			blobStorageContainers.put(container_name, container);
		}

		return container;
	}

	public static CloudBlobContainer getContainer(String container_name) throws StorageException, InvalidKeyException, URISyntaxException {
		return blobStorageContainers.get(container_name);
	}

	public static void upload(String container_name, String blob_id, byte[] contents) throws Exception {
		// Get reference to blob
		CloudBlob blob = getContainer(container_name).getBlockBlobReference(blob_id);

		// Upload contents from byte array
		blob.uploadFromByteArray(contents, 0, contents.length);
	}

	public static byte[] download(String container_name, String blob_id) throws StorageException, InvalidKeyException, URISyntaxException, IOException {
		// Get reference to blob
		CloudBlob blob = getContainer(container_name).getBlobReferenceFromServer(blob_id);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		blob.download(out);
		out.close();
		byte[] contents = out.toByteArray();

		return contents;
	}



}

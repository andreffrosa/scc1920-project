package scc.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

public class BlobStorageClient {

	private static CloudStorageAccount storageAccount;
	private static CloudBlobClient blobClient;

	private BlobStorageClient () {}

	public static void init(String connection_string) {
		try {
			storageAccount = CloudStorageAccount.parse(connection_string);
			blobClient = storageAccount.createCloudBlobClient();
		} catch (InvalidKeyException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public static void upload(String container_name, String blob_id, byte[] contents) throws URISyntaxException, StorageException, IOException {
		CloudBlobContainer container = blobClient.getContainerReference(container_name);
		
		// Get reference to blob
		CloudBlob blob = container.getBlockBlobReference(blob_id);

		// Upload contents from byte array
		blob.uploadFromByteArray(contents, 0, contents.length);
	}

	public static byte[] download(String container_name, String blob_id) throws StorageException, InvalidKeyException, URISyntaxException, IOException {
		CloudBlobContainer container = blobClient.getContainerReference(container_name);
		
		// Get reference to blob
		CloudBlob blob = container.getBlobReferenceFromServer(blob_id);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		blob.download(out);
		out.close();
		byte[] contents = out.toByteArray();

		return contents;
	}



}

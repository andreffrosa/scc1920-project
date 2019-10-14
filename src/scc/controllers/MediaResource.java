    package scc.controllers;

import java.io.ByteArrayOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import scc.utils.Encryption;

@Path("/media")
public class MediaResource {

	private static final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=blob48043;AccountKey=+0Iy0fw08l+l2A/ZNUY6qPKlLsKZ+mrrq1KA7xSC3x18tzAdzue/ysQd3NxiPDeXP5RC+fMr9Ke1gRuIDzKsZw==;EndpointSuffix=core.windows.net";
	private static final String containerName = "images";
	
	private CloudBlobContainer container;
	
	public MediaResource() throws Exception {
		// Get connection string in the storage access keys page
		CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
		CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
		container = blobClient.getContainerReference(containerName);

	}
	
	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public Response upload(byte[] contents) {
		try {
			String hash = Encryption.computeHash(contents);
			
			// Get reference to blob
			CloudBlob blob = container.getBlockBlobReference(hash);
			
			// Upload contents from byte array
			blob.uploadFromByteArray(contents, 0, contents.length);
			
			return Response.ok(hash, MediaType.APPLICATION_JSON).build();
		} catch(Exception e) {
			return Response.serverError().entity(e).build();
		}
	}

	@GET
	@Path("/{uid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response download(@PathParam("uid") String uid) {
		try {
			// Get reference to blob
			CloudBlob blob = container.getBlobReferenceFromServer(uid);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			blob.download(out);
			out.close();
			byte[] contents = out.toByteArray();
			
			return Response.ok(contents, MediaType.APPLICATION_OCTET_STREAM).build();
		} catch(Exception e) {
			return Response.serverError().entity(e).build();
		}
	}

}

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
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageErrorCode;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import scc.utils.Encryption;

@Path(ImageResource.PATH)
public class ImageResource {

	public static final String PATH = "/images";
	
	private static final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=storage48043and47134;AccountKey=D45zU5e7UKm+GqyYZKnssw9rZh0PA0xC5lhcpdjW0Bhdwqubrgjx63RJSUXl2yaTK8wAiUdIEmXBKCcwLsahvA==;EndpointSuffix=core.windows.net";
	private static final String containerName = "images";
	
	private CloudBlobContainer container;
	
	public ImageResource() throws Exception {
		// Get connection string in the storage access keys page
		CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
		CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
		container = blobClient.getContainerReference(containerName);

	}
	
	@POST
	@Path("/")
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
	@Path("/{uuid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response download(@PathParam("uuid") String uuid) {
		try {
			// Get reference to blob
			CloudBlob blob = container.getBlobReferenceFromServer(uuid);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			blob.download(out);
			out.close();
			byte[] contents = out.toByteArray();
			
			return Response.ok(contents, MediaType.APPLICATION_OCTET_STREAM).build();
			
		} catch (StorageException e) {
	        if (e.getErrorCode().equals(StorageErrorCode.RESOURCE_NOT_FOUND.toString()))
	        	return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
	        
	        return Response.serverError().entity(e).build();
	    } catch(Exception e) {
			return Response.serverError().entity(e).build();
		}
	}

}

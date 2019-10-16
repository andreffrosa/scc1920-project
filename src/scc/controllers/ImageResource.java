    package scc.controllers;

	import com.microsoft.azure.storage.StorageErrorCode;
	import com.microsoft.azure.storage.StorageException;
	import com.microsoft.azure.storage.blob.CloudBlob;
	import scc.storage.blobStorage.BlobStorageSingleton;
	import scc.utils.Encryption;
	import scc.storage.config.Config;

	import javax.servlet.ServletContext;
	import javax.ws.rs.*;
	import javax.ws.rs.core.Context;
	import javax.ws.rs.core.MediaType;
	import javax.ws.rs.core.Response;
	import javax.ws.rs.core.Response.Status;
	import java.io.ByteArrayOutputStream;

@Path(ImageResource.PATH)
public class ImageResource {
	static final String PATH = "/image";
	private static final String CONTAINER_NAME = "images";

	@Context
	static ServletContext context;

	private BlobStorageSingleton blobStorageSingleton;

	public ImageResource() throws Exception {
		blobStorageSingleton = Config.getBlobStorageClientInstance(CONTAINER_NAME);
	}
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public Response upload(@Context ServletContext context, byte[] contents) {
		try {
			String hash = Encryption.computeHash(contents);

			// Get reference to blob
			CloudBlob blob = blobStorageSingleton.getContainer().getBlockBlobReference(hash);
			
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
			CloudBlob blob = blobStorageSingleton.getContainer().getBlobReferenceFromServer(uuid);
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

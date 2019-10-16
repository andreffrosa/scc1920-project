    package scc.controllers;

	import com.microsoft.azure.storage.StorageErrorCode;
	import com.microsoft.azure.storage.StorageException;
	import com.microsoft.azure.storage.blob.CloudBlob;

import scc.utils.Encryption;
import scc.storage.BlobStorageClient;
import scc.storage.Config;

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
	public static final String CONTAINER_NAME = "images";

	public ImageResource() {}
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public String upload(@Context ServletContext context, byte[] contents) {
		try {
			String hash = Encryption.computeHash(contents);
			
			BlobStorageClient.upload(CONTAINER_NAME, hash, contents);
			
			return hash;
		} catch(Exception e) {
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

	@GET
	@Path("/{img_id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] download(@PathParam("img_id") String img_id) {
		try {
			return BlobStorageClient.download(CONTAINER_NAME, img_id);
		} catch (StorageException e) {
	        if (e.getErrorCode().equals(StorageErrorCode.RESOURCE_NOT_FOUND.toString()))
	        	throw new WebApplicationException(e.getMessage(), Status.NOT_FOUND);
	        
	        throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
	    } catch(Exception e) {
	    	throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

}

package scc.controllers;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.storage.StorageErrorCode;
import com.microsoft.azure.storage.StorageException;

import scc.storage.BlobStorageClient;
import scc.utils.Encryption;

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

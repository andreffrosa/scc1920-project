package scc.controllers;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.storage.StorageErrorCode;
import com.microsoft.azure.storage.StorageException;

import scc.storage.BlobStorageClient;
import scc.storage.Redis;
import scc.utils.Encryption;
import scc.utils.MyBase64;

@Path(ImageResource.PATH)
public class ImageResource extends Resource {

	public static final String PATH = "/image";
	public static final String CONTAINER_NAME = "images";

	//public ImageResource() {}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public static String upload(byte[] contents) {
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
	public static byte[] download(@PathParam("img_id") String img_id) {
		try {
			byte[] img = null;
			
			String img_base64 = Redis.LRUDictionaryGet(Redis.TOP_IMAGES, img_id);
			if(img_base64 == null) {
				img = BlobStorageClient.download(CONTAINER_NAME, img_id);
				
				Redis.LRUDictionaryPut(Redis.TOP_IMAGES, Redis.TOP_IMAGES_LIMIT, img_id, MyBase64.encode(img));
			} else
				img = MyBase64.decode(img_base64);
			
			return img;
		} catch (StorageException e) {
			if (e.getErrorCode().equals(StorageErrorCode.RESOURCE_NOT_FOUND.toString()))
				throw new WebApplicationException(e.getMessage(), Status.NOT_FOUND);

			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		} catch(Exception e) {
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

}

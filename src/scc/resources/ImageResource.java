package scc.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.storage.StorageErrorCode;
import com.microsoft.azure.storage.StorageException;

import scc.storage.BlobStorageClient;
import scc.storage.Redis;
import scc.utils.Config;
import scc.utils.Encryption;
import scc.utils.MyBase64;

public class ImageResource {
	
	public static byte[] download(String img_id) {
		try {
			byte[] img = null;
			
			String img_base64 = Redis.LRUDictionaryGet(Config.TOP_IMAGES, img_id);
			if(img_base64 == null) {
				img = BlobStorageClient.download(Config.IMAGES_CONTAINER, img_id);
				
				Redis.LRUDictionaryPut(Config.TOP_IMAGES, Integer.parseInt(Config.getRedisProperty(Config.TOP_IMAGES_LIMIT)), img_id, MyBase64.encode(img));
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
	
	public static String upload(byte[] contents) {
		try {
			String hash = Encryption.computeHash(contents);

			BlobStorageClient.upload(Config.IMAGES_CONTAINER, hash, contents);

			return hash;
		} catch(Exception e) {
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

}

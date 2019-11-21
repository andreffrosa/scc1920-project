package scc.endpoints;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import scc.resources.ImageResource;

@Path(ImageEndpoint.PATH)
public class ImageEndpoint {

	public static final String PATH = "/image";

	public ImageEndpoint() {
		super();
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public String upload(byte[] contents) {
		return ImageResource.upload(contents);
	}

	@GET
	@Path("/{img_id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] download(@PathParam("img_id") String img_id) {
		return ImageResource.download(img_id);
	}

}

package scc.controllers;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.cosmosdb.DocumentClientException;

import scc.models.Community;

@Path(CommunityResource.PATH)
public class CommunityResource extends Resource {

	public static final String PATH = "/community";
	static final String CONTAINER = "Communities";

	public CommunityResource() throws Exception {
		super(CONTAINER);
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String createCommunity(Community c) {

		if(!c.isValid())
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Params").build());

		try {
			c.setCreation_date(System.currentTimeMillis());
			return super.create(c);
		} catch(DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException(Response.status(Status.CONFLICT).entity("Community with the specified name already exists in the system.").build());
		
			throw new WebApplicationException( Response.serverError().entity(e.getMessage()).build());
		}
	}

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public String consultCommunity(@PathParam("name") String name) {
		String community = super.getByName(name);
		
		if(community == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("Community with the specified name does not exists.").build());
		
		return community;
	}
}


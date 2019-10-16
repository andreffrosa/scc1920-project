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

import com.microsoft.azure.cosmosdb.DocumentClientException;

import scc.models.Community;

@Path(CommunityResource.PATH)
public class CommunityResource extends Resource {

	@Context
	static ServletContext context;

	static final String PATH = "/community";
	public static final String CONTAINER = "Communities";

	public CommunityResource() throws Exception {
		super(CONTAINER);
	}

	// TODO: Fazer as excepções como deve ser
	// TODO: Fazer as replys como o prof tem nos slides
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String createCommunity(Community c) {
		try {
			return super.create(c);
		} catch(DocumentClientException e) {
			if(e.getStatusCode() == 409)
				throw new WebApplicationException("Community with the specified name already exists in the system.", Status.CONFLICT);
		
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

	//TODO: GET DE COISA QUE NÂO EXISTE LEVA INFINITO TEMPO E RETORNA There was an unexpected error in the request processing.
	
	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public String consultCommunity(@PathParam("name") String name) {
		String community = super.getByName(name);
		
		if(community == null)
			throw new WebApplicationException("Community with the specified name does not exists.", Status.NOT_FOUND);
		
		return community;
	}


	/*@PUT
	@Path("/{name}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(Community c){
		return super.update(c);
	}

	@DELETE
	@Path("/{name}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("name") String name){
		return super.delete(name);
	}*/

}


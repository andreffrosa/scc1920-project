package scc.controllers;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.cosmosdb.internal.directconnectivity.ConflictException;

import scc.models.Community;

@Path(CommunityResource.PATH)
public class CommunityResource extends Resource {
	
	public static final String PATH = "/community";
	private static final String CONTAINER = "Communities";

	public CommunityResource() throws Exception {
		super(CONTAINER);
	}

	// TODO: Fazer as excepções como deve ser
	// TODO: Fazer as replys como o dred tem nos slides
	
	@POST
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createCommunity(@PathParam("name") String name) {
		return super.create(new Community(name), 
				response -> Response.ok(response.getResource().getId(), MediaType.APPLICATION_JSON).build(),
				error -> {
					if(error instanceof ConflictException)
						return Response.status(Status.CONFLICT)
								.entity("Community with the specified name already exists in the system.")
								.build();
					
					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(error.getMessage())
							.build();
				});
	}

	//TODO: GET DE COISA QUE NÂO EXISTE LEVA INFINITO TEMPO E RETORNA There was an unexpected error in the request processing.
	
	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response consultCommunity(@PathParam("name") String name) {
		return super.findByName(name);
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


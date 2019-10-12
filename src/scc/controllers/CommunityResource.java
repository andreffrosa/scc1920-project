package scc.controllers;

import com.microsoft.azure.cosmosdb.User;
import scc.models.Community;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/community")
public class CommunityResource extends Resource {

	public CommunityResource() throws Exception {
	    super(Community.DataType);
	}

	// TODO: Fazer as excepções como deve ser
	// TODO: Fazer as replys como o dred tem nos slides
	
	@POST
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createCommunity(@PathParam("name") String name) {

		return super.create(new User(name));

		/*
		try {
			Observable<ResourceResponse<Document>> resp =
					cosmos_client.createDocument(CommunitiesCollection, new Community(name), null, false);

			return Response.ok(resp.toBlocking().first().getResource().getId(), MediaType.APPLICATION_JSON).build();
		} catch(Exception e) {

			// TODO: Perguntar como capturar as excepções como deve ser
			if(e.getMessage().contains("statusCode=409"))
				return Response.status(Status.CONFLICT).entity("Community with the specified name already exists in the system.").build();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		 */
	}

	//TODO: GET DE COISA QUE NÂO EXISTE LEVA INFINITO TEMPO E RETORNA There was an unexpected error in the request processing.
	
	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response consultCommunity(@PathParam("name") String name) {
		return super.findByName(name);
	}


	@PUT
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
	}

}


package scc.controllers;

import scc.models.User;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.cosmosdb.internal.directconnectivity.ConflictException;

@Path(UserResouce.PATH)
public class UserResouce extends Resource {

	public static final String PATH = "/user";
	private static final String CONTAINER = "Users";
	
	public UserResouce() throws Exception {
		super(CONTAINER);
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(User u){
		return super.create(u, 
				response -> {
					return Response.ok(response.getResource().getId(), MediaType.APPLICATION_JSON).build();
				}, error -> {
					if(error instanceof ConflictException)
						return Response.status(Status.CONFLICT)
								.entity("User with the specified name already exists in the system.")
								.build();

					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(error.getMessage())
							.build();
				});
	}

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findByName(@PathParam("name") String name){
		return super.findByName(name);
	}

	/*@PUT
    @Path("/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(User u){
        return super.update(u);
    }

    @DELETE
    @Path("/{name}")
    public Response delete(@PathParam("name") String name){
        return super.delete(name);
    }*/
}

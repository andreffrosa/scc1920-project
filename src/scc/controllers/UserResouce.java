package scc.controllers;

import com.microsoft.azure.cosmosdb.DocumentClientException;
import scc.models.User;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

@Path(UserResouce.PATH)
public class UserResouce extends Resource {

	static final String PATH = "/user";
	private static final String CONTAINER = "Users";

	@Context
	static ServletContext context;

	public UserResouce() throws Exception {
		super(CONTAINER);
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(User u){
		try {
			return super.create(u);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException("User already exists", Status.CONFLICT);
			else
				throw new WebApplicationException("Unexpected error", Status.INTERNAL_SERVER_ERROR);
		}
	}

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getByName(@PathParam("name") String name){
		String user = super.getByName(name);

		if(user == null)
			throw new WebApplicationException("User not found", Status.NOT_FOUND);

		return super.getByName(name);
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

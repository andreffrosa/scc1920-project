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

import scc.models.User;
import scc.storage.CosmosClient;

@Path(UserResource.PATH)
public class UserResource extends Resource {

	public static final String PATH = "/user";
	static final String CONTAINER = "Users";

	public UserResource() throws Exception {
		super(CONTAINER);
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String create(User u){

		if(!u.isValid())
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Params").build());

		try {
			return super.create(u);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity("User already exists").build() );
			else
				throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getByName(@PathParam("name") String name){
		String user = super.getByName(name);

		if(user == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("User not found").build());

		return super.getByName(name);
	}

	public static boolean userExists(String username){
		return CosmosClient.getByName(UserResource.CONTAINER, username) != null;
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

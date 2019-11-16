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
import scc.utils.GSON;

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
	public String create(User user){
		if(!user.isValid())
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Invalid Parameters").build());

		try {
			return CosmosClient.create(super.collection, user);
		} catch (DocumentClientException e) {
			if(e.getStatusCode() == Status.CONFLICT.getStatusCode())
				throw new WebApplicationException( Response.status(Status.CONFLICT).entity(String.format("User %s already exists", user.getName())).build() );
			else
				throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}
	}

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getByName(@PathParam("name") String name){
		User user = CosmosClient.getByNameUnparse(super.collection, name, User.class);
		
		if(user == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity(String.format("User %s not found", name)).build());

		return GSON.toJson(user);
	}

	public static boolean exists(String username) {
		return CosmosClient.getByName(CONTAINER, username) != null;
	}

}

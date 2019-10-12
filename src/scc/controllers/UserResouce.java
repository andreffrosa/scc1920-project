package scc.controllers;

import scc.models.User;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static scc.models.User.DataType;

@Path("/user")
public class UserResouce extends Resource{

    public UserResouce() throws Exception {
        super(DataType);
    }

    @POST
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@PathParam("name") String name){
        try {
            return super.create(new User(name));
        }catch (Exception e){
            return Response.serverError().entity(e).build();
        }
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findByName(@PathParam("name") String name){
        return super.findByName(name);
    }

    @PUT
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
    }
}

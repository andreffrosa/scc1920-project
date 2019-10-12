package scc.controllers;

import scc.models.Post;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/post")
public class PostResource extends Resource{

    public PostResource() {
        super(Post.DataType);
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Post u){
        return super.create(u);
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
    public Response update(Post u){
        return super.update(u);
    }

    @DELETE
    @Path("/{name}")
    public Response delete(@PathParam("name") String name){
        return super.delete(name);
    }
}

package scc.controllers;

import com.microsoft.azure.cosmosdb.DocumentClientException;
import scc.models.Like;
import scc.models.Post;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.microsoft.azure.cosmosdb.internal.directconnectivity.ConflictException;

@Path(PostResource.PATH)
public class PostResource extends Resource{

	public static final String PATH = "/post";
	private static final String CONTAINER = "Posts";
	
	public PostResource() throws Exception {
		super(CONTAINER);
	}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Post post){
    	return super.create(post,
				response -> Response.ok(response.getResource().getId(), MediaType.APPLICATION_JSON).build(),
				error -> {
					if(error instanceof ConflictException)
						return Response.status(Status.CONFLICT)
								.entity("Post with the specified name already exists in the system.")
								.build();

					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(error.getMessage())
							.build();
				});
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findById(@PathParam("id") String id){
        return super.findByName(id);
    }


    @POST
	@Path("/{id}/like/{user_id}")
	public Response likePost(@PathParam("id") int postId, @PathParam("user_id") String user_id){
		return super.create( new Like(postId, user_id),
				response -> Response.ok().build(),
				error -> {

			        if(error instanceof DocumentClientException) {
						int statusCode = ((DocumentClientException) error).getStatusCode();
			        	if (statusCode == Status.CONFLICT.getStatusCode())
							return Response.status(statusCode)
								.entity("Already has like on Post.")
								.build();
			        	else if (statusCode == Status.NOT_FOUND.getStatusCode())
			        		return Response.status(statusCode)
								.entity("Post with id " + postId + " does not exist")
								.build();
					}
					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(error.getMessage())
							.build();
				});
	}

    /*@PUT
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
    }*/
}

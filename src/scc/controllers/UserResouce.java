package scc.controllers;

import com.microsoft.azure.cosmosdb.ConnectionMode;
import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import scc.models.User;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/users")
public class UserResouce extends Resource{

    static final String USER_RESOURCE_PATH = "/user";

    private static final String COSMOS_DB_ENDPOINT = "https://cloud-1920.documents.azure.com:443/";
    private static final String COSMOS_DB_MASTER_KEY ="d2uk6OuA3b8jzqXBIK2yhgw9VVKMBhxpp3zXUi5uG2v3U6pTI1M2W9wUBjQ1gFIcGOnnlJbRmCSZtWPRchBk6Q=="; // "cmqCzSEYRX5E2GLF2kxF3ftlEXZnZLLCRA4nZvb4jpH5gLZN6oWiPtGpLWx2l2iRvQ0IHwA8DmKPq33KqdNwog==";
    private static final String COSMOS_DB_DATABASE =  "cloud-2019";// "scc1920-48043";

    private static final String DataType = "Communities";

    public UserResouce() throws Exception {
        super(DataType);
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(User u){
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
    public Response update(User u){
        return super.update(u);
    }
}

package scc.controllers;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Properties;

@Path(Debug.PATH)
public class Debug {

    @Context ServletContext context;
    static final String PATH = "/debug" ;
    private static final String VERSION = "24.0";

    @GET
    @Path("/version")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersion(){
        return Response.ok(VERSION).build();
    }

    @GET
    @Path("/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readFile(){
        try {
            InputStream is = context.getResourceAsStream("./config/Cosmos.conf");
            Properties properties = new Properties();
            properties.load(is);
            return Response.ok(properties.getProperty("cosmos_db_database")).build();
        } catch (Exception e){
            return Response.serverError().entity(e).build();
        }
    }
}

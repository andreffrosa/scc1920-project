package scc.config.Exceptions;

public class EndpointURLNotFound extends Exception{

    public EndpointURLNotFound(){
        super("You have to provide the URL of your resource");
    }

    public EndpointURLNotFound(String message){
        super(message);
    }

}

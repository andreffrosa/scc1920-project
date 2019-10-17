package scc.storage.Exceptions;

public class EndpointURLNotFound extends Exception{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EndpointURLNotFound(){
        super("You have to provide the URL of your resource");
    }

    public EndpointURLNotFound(String message){
        super(message);
    }

}

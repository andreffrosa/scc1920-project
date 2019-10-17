package scc.storage.Exceptions;

public class MasterKeyNotFound extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MasterKeyNotFound(){
        super("You need to provide your CosmosDb Master Key");
    }

    public MasterKeyNotFound(String message){
        super(message);
    }
}

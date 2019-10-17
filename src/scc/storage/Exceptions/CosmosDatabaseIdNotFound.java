package scc.storage.Exceptions;

public class CosmosDatabaseIdNotFound extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CosmosDatabaseIdNotFound(){
        super("You have to provide your CosmosDB ID");
    }

    public CosmosDatabaseIdNotFound(String message){
        super(message);
    }
}

package scc.Config.Exceptions;

public class CosmosDatabaseIdNotFound extends Exception {

    public CosmosDatabaseIdNotFound(){
        super("You have to provide your CosmosDB ID");
    }

    public CosmosDatabaseIdNotFound(String message){
        super(message);
    }
}

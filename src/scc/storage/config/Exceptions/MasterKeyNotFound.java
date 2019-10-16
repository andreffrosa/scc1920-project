package scc.storage.config.Exceptions;

public class MasterKeyNotFound extends Exception {

    public MasterKeyNotFound(){
        super("You need to provide your CosmosDb Master Key");
    }

    public MasterKeyNotFound(String message){
        super(message);
    }
}

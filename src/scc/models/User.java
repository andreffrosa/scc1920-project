package scc.models;

public class User {

    private String id;
    private String name;

    public User(String name){
        id = null;
        this.name = name;
    }

    public String getName(){
        return name;
    }

	public String getId() {
		return id;
	}

}

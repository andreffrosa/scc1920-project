package scc.models;

public class User {

    public static final String DataType = "Users";

    private String id;
    private String name;

    public User(String name){
        id = null;
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

}

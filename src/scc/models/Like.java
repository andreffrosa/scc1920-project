package scc.models;

public class Like {

    private String id, post_id;
    private String author_username;

    public Like(){  }

    public Like(String post_id, String author_username){
        this.post_id = post_id;
        this.author_username = author_username;
    }

    public String getId(){ return id; }

    public String getPost_id(){ return  post_id; }

    public String getAuthor_username(){ return  author_username; }
}

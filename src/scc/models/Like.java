package scc.models;

public class Like extends Resource {

    private String post_id;
    private String username;

    public Like(String post_id, String author_username) {
        super();
        this.post_id = post_id;
        this.username = author_username;
    }

    public String getPost_id(){ return  post_id; }

    public String getAuthor_username(){ return  username; }


	public static String buildId(String post_id, String author_username){
        return author_username + "@" + post_id;
    }

    public boolean isValid(){ return username != null && !username.equals("") && post_id != null && !post_id.equals(""); }

}


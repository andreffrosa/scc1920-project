package scc.models;

public class Like {

    private String id, post_id;
    private String author_username;
    private long creationTime;

    public Like(String post_id, String author_username, long creation_time){
        id = buildId(post_id, author_username);
        this.post_id = post_id;
        this.author_username = author_username;
        this.creationTime = creation_time;
    }

    public String getId(){ return id; }

    public String getPost_id(){ return  post_id; }

    public String getAuthor_username(){ return  author_username; }

    public void setCreationTime(Long creationTime){
        this.creationTime = creationTime;
    }

	public long getCreationTime() {
		return creationTime;
	}

	public static String buildId(String post_id, String author_username){
        return author_username + "@" + post_id;
    }
    
}

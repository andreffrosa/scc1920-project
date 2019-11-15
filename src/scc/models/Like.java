package scc.models;

public class Like {

    private String id, post_id;
    private String username;
    private long _ts;

    public Like(String post_id, String author_username) {
        this.id = buildId(post_id, author_username);
        this.post_id = post_id;
        this.username = author_username;
        this._ts = 0;
    }

    public String getId(){ return id; }

    public String getPost_id(){ return  post_id; }

    public String getAuthor_username(){ return  username; }

    public void setCreationTime(long creation_time){
        this._ts = creation_time;
    }

	public long getCreationTime() {
		return _ts;
	}

	public static String buildId(String post_id, String author_username){
        return author_username + "@" + post_id;
    }

    public boolean isValid(){ return username != null && !username.equals("") && post_id != null && !post_id.equals(""); }
    
}


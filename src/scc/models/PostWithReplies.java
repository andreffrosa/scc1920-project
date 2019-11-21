package scc.models;

import java.util.List;

public class PostWithReplies extends Post {

	private long n_likes;
    private List<PostWithReplies> replies;
    private String continuation_token;
   
    public PostWithReplies() {
    	super();
    }
    
    public PostWithReplies(Post p) {
    	super(p.getTitle(), p.getAuthor(), p.getCommunity(), p.getMessage(), p.getMultiMediaObject(), p.getParent());
    }

	public List<PostWithReplies> getReplies() {
		return replies;
	}

	public void setReplies(List<PostWithReplies> replies) {
		this.replies = replies;
	}

	public void setLikes(long n_likes) {
		this.n_likes = n_likes;
	}

	public long getLikes() {
		return n_likes;
	}

	public String getContinuationToken() {
		return continuation_token;
	}

	public void setContinuationToken(String continuationToken) {
		this.continuation_token = continuationToken;
	}
	
}

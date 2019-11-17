package scc.models;

import java.util.List;

public class PostWithReplies extends Post {

	private long n_likes;
    private List<PostWithReplies> replies;
    private String continuationToken;
   
    public PostWithReplies() {}

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
		return continuationToken;
	}

	public void setContinuationToken(String continuationToken) {
		this.continuationToken = continuationToken;
	}
	
}

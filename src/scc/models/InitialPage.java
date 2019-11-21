package scc.models;

import java.util.List;

public class InitialPage {
	
	// TODO: Esta class não está a ser utilizada

	private List<PostWithReplies> posts;
	private String continuationToken;
	
	public InitialPage(List<PostWithReplies> posts, String continuationToken) {
		this.posts = posts;
		this.continuationToken = continuationToken;
	}
	public List<PostWithReplies> getPosts() {
		return posts;
	}
	public void setPosts(List<PostWithReplies> posts) {
		this.posts = posts;
	}
	public String getContinuationToken() {
		return continuationToken;
	}
	public void setContinuationToken(String continuationToken) {
		this.continuationToken = continuationToken;
	}
	
	
	
}

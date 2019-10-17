package scc.models;

import java.util.List;

public class PostWithReplies extends Post {

    private List<PostWithReplies> replies;
    private int n_likes;

    public PostWithReplies() {}

	public List<PostWithReplies> getReplies() {
		return replies;
	}

	public void setReplies(List<PostWithReplies> replies) {
		this.replies = replies;
	}

	public void setLikes(int n_likes) {
		this.n_likes = n_likes;
	}

	public int getLikes() {
		return n_likes;
	}
	
}

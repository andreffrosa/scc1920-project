package scc.models;

import java.util.List;

public class Post {

    private String id;
    private String title;
    private String author;
    private String community;
    private long creationTime;
    public String message;
    private String multiMediaObject;
    private String parent;
    private List<String> replies;

    public Post() {   }

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getCommunity() {
		return community;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public String getMessage() {
		return message;
	}

	public String getMultiMediaObject() {
		return multiMediaObject;
	}

	public String getParent() {
		return parent;
	}

	public void setCreationTime(Long creationTime){
    	this.creationTime = creationTime;
	}

	public List<String> getReplies() {
		return replies;
	}

	public void setReplies(List<String> replies) {
		this.replies = replies;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setCommunity(String community) {
		this.community = community;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setMultiMediaObject(String multiMediaObject) {
		this.multiMediaObject = multiMediaObject;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

}

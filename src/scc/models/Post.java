package scc.models;

public class Post extends Resource {

	private String title;
	private String author;
	private String community;
	private String message;
	private String image;
	private String parent;

	public Post() {
		super();
	}

	public Post(String title, String author, String community, String message, String multiMediaObject, String parent) {
		super();
		this.title = title;
		this.author = author;
		this.community = community;
		this.message = message;
		this.image = multiMediaObject;
		this.parent = parent;
	}

	public String toString(){
		return "title: " + title + "\n"+
				"author: " + author + "\n" +
				"community: " + community + "\n" +
				"message: " + message + "\n" +
				"image: " + ( image != null ? image : "null" ) + "\n" +
				"parent: " + (parent != null ? parent : "null");

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

	public String getMessage() {
		return message;
	}

	public String getImage() {
		return image;
	}

	public String getParent() {
		return parent;
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

	public void setMessage(String message) {
		this.message = message;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public boolean validPost(){
		return community!= null && !community.equals("") 
				&& author != null && !author.equals("") 
				&& message != null && !message.equals("");
	}

	public boolean hasValidParent(){
		return parent != null && !parent.equals("");
	}

	public boolean isReply(){ 
		return parent != null; 
	}

	public void correctPost() {
        if( this.parent != null && this.parent.equals("") )
            this.parent = null;
        
        if( this.title != null &&this.title.equals("") )
        	this.title = null;
		
        if( this.image != null && this.image.equals("") )
        	this.image = null;
	}

}
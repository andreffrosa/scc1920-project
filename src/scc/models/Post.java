package scc.models;

public class Post extends Resource {

	private String title;
	private String author;
	private String community;
	private String message;
	private String multiMediaObject;
	private String parent;

	public Post() {
		super("/community");
	}

	public Post(String title, String author, String community, String message, String multiMediaObject, String parent) {
		super(null, null, "/community");
		this.id = null;
		this.title = title;
		this.author = author;
		this.community = community;
		this.message = message;
		this.multiMediaObject = multiMediaObject;
		this.parent = parent;
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

	public String getMultiMediaObject() {
		return multiMediaObject;
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

	public void setMultiMediaObject(String multiMediaObject) {
		this.multiMediaObject = multiMediaObject;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public boolean validPost(){
		return community!= null && !community.equals("") && author != null && !author.equals("") && title != null && !title.equals("") && message != null && !message.equals("");
	}

	public boolean validReply(){
		return author != null && !author.equals("") && parent != null && !parent.equals("") && message != null && !message.equals("");
	}

	public boolean isReply(){ return parent != null; }

}

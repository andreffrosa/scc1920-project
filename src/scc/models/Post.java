package scc.models;

public class Post {

    private String id;
    private String title;
    private String author;
    private String community;
    private long creationTine;
    public String message;
    private String multiMediaObject;
    private String parent;

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

	public long getCreationTine() {
		return creationTine;
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

}

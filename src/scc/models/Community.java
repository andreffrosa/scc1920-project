package scc.models;

public class Community {

	private String id;
	private String name;
	private long _ts;
	//private String description;
	//private String img;

	public Community() {}

	public Community(String name) {
		this.name = name;
		this._ts = 0;
		this.id = null;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isValid() {
		return name != null && !name.equals("");
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	 public void setCreationTime(long creation_time){
	        this._ts = creation_time;
	    }

	public long getCreationTime() {
		return _ts;
	}
	
}

package scc.models;

public class User {

	private String id;
	private String name;
	private Long _ts;

	public User() {}

	public User(String name) {
		this.id = null;
		this.name = name;
		this._ts = null;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}
	
    public void setCreationTime(long creation_time){
        this._ts = creation_time;
    }

	public long getCreationTime() {
		return _ts == null ? -1 : _ts.longValue();
	}

	public boolean isValid() {
		return name != null && !name.equals("");
	}

}

package scc.models;

public class Resource {

	protected String id;
	protected Long _ts;

	public Resource() {}

	public Resource(String id, Long _ts) {
		this.id = id;
		this._ts = _ts;
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

}

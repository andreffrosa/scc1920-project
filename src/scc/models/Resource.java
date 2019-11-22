package scc.models;

public class Resource {

	protected String id;
	protected Long _ts;
	protected String partition_key;
	
	public Resource (String partition_key) {
		this.partition_key = partition_key;
	}
	
	protected Resource(String id, Long _ts, String partition_key) {
		this.id = id;
		this._ts = _ts;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setCreationTime(long creation_time){
		this._ts = creation_time;
	}

	public long getCreationTime() {
		return _ts == null ? -1 : _ts.longValue();
	}

	public String getPartition_key() {
		return partition_key;
	}

	public void setPartition_key(String partition_key) {
		this.partition_key = partition_key;
	}
	
}

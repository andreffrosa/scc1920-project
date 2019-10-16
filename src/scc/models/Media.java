package scc.models;

public class Media {
	
	private String id;

	//Dummy Constructor so that jackson can Deserialize
	public Media(){ }

	public Media(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}

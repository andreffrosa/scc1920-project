package scc.models;

public class Community {

	private String id;
	private String name;

	//Dummy Constructor so that jackson can Deserialize
	public Community(){ }

	public Community(String name) {
		this.id = null;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
}

package scc.models;

public class Community {

	public static final String DataType = "Communities";

	private String id;
	private String name;
	
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

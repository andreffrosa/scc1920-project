package scc.models;

public class User {

	private String id;
	private String name;

	public User() {}

	public User(String name) {
		this.id = null;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public boolean isValid() {
		return name != null && !name.equals("");
	}

}

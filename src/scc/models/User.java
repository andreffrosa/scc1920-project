package scc.models;

public class User extends Resource{

	private String name;

	public User() {}

	public User(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isValid() {
		return name != null && !name.equals("");
	}

}

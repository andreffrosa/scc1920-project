package scc.models;

public class Community extends Resource {

	private String name;

	public Community() {
		super();
	}

	public Community(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isValid() {
		return name != null && !name.equals("");
	}

	public void setName(String name) {
		this.name = name;
	}

}

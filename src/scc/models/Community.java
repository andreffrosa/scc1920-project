package scc.models;

public class Community {

	private String id;
	private String name;
	private long creation_date;
	//private String description;
	//private String img;

	public Community() {}

	public Community(String name) {
		this.name = name;
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

	public void setCreation_date(long creation_date) {
		this.creation_date = creation_date;
	}

	public long getCreation_date() {
		return creation_date;
	}
	
}

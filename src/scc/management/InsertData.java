package scc.management;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.github.javafaker.Faker;

import scc.controllers.Debug;

public class InsertData {
	
	private final static int CONNECT_TIMEOUT = 95000;
	private final static int READ_TIMEOUT = 90000;
	
	private final static String LOCATION = "";

	public static void main(String[] args) {
		ClientConfig config = new ClientConfig();
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
		config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
		Client client = ClientBuilder.newBuilder().withConfig(config).build();
		WebTarget target = client.target(LOCATION);

		Response response = target.path(Debug.PATH + "/version")
				.request()
				.get();
		
		if (response.getStatus() == 200) {
			System.out.println(response.readEntity(String.class));
		} else
			throw new RuntimeException("WalletClient Transfer: " + response.getStatus());
		
		Faker faker = new Faker();
		
		// TODO
		
	}
	
}

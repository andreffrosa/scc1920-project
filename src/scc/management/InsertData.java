package scc.management;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.github.javafaker.Faker;

import scc.models.Community;
import scc.models.Post;
import scc.models.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import scc.controllers.CommunityResource;
import scc.controllers.Debug;
import scc.controllers.ImageResource;
import scc.controllers.PostResource;
import scc.controllers.UserResource;

public class InsertData {

	private final static int CONNECT_TIMEOUT = 95000;
	private final static int READ_TIMEOUT = 90000;

	private final static String LOCATION = "https://scc-backend-euwest-app-47134.azurewebsites.net";

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

		Faker faker = new Faker(new Locale("en-US"));

		System.out.println("Starting...");

		//List<User> users = new ArrayList<>(50);
		int n_users = 100;
		int n_communities = 30;
		int n_posts = 300;
		int n_likes = 500;
		int n_images = 0;

		List<String> _images = new ArrayList<>(50);

		File folder = new File("./images");
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.isFile()) {
				Path fileLocation = Paths.get(fileEntry.getPath());
				try {
					byte[] data = Files.readAllBytes(fileLocation);

					response = target.path(ImageResource.PATH + "/")
							.request()
							.post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));

					if(response.getStatus() == 200) {
						System.out.println("Created user " + fileEntry.getName() + " sucessfully!");
						_images.add(response.readEntity(String.class));
					} else
						System.out.println("Created user " + fileEntry.getName() + " error: " + response.getStatus());

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		String[] images = _images.toArray(new String[_images.size()]);

		User[] users = new User[n_users];
		for(int i = 0; i < n_users; i++) {
			users[i] = new User(faker.name().fullName());
			response = target.path(UserResource.PATH + "/")
					.request()
					.post(Entity.entity(users[i], MediaType.APPLICATION_JSON));

			if(response.getStatus() == 200)
				System.out.println("Created user " + users[i].getName() + " sucessfully!");
			else
				System.out.println("Created user " + users[i].getName() + " error: " + response.getStatus());
		}


		Community[] communities = new Community[n_communities];
		for(int i = 0; i < n_communities; i++) {
			communities[i] = new Community(faker.address().country().replace(" ", "_").toLowerCase());
			response = target.path(CommunityResource.PATH + "/")
					.request()
					.post(Entity.entity(communities[i], MediaType.APPLICATION_JSON));

			if(response.getStatus() == 200)
				System.out.println("Created community " + communities[i].getName() + " sucessfully!");
			else
				System.out.println("Created community " + communities[i].getName() + " error: " + response.getStatus());
		}

		Post[] posts = new Post[n_posts];
		for(int i = 0; i < n_posts; i++) {
			String title = faker.book().title();
			String author = users[(int)(Math.random()*n_users)].getName();
			String community = communities[(int)(Math.random()*n_communities)].getName();
			String message = faker.shakespeare().romeoAndJulietQuote();
			String media = Math.random() <= 0.7 || i == 0 ? null : images[(int)(Math.random()*n_images)];
			String parent = Math.random() <= 0.3 || i == 0 ? null : posts[(int)(Math.random()*(i-1))].getId();

			posts[i] = new Post(title, author, community, message, media, parent);
			response = target.path(PostResource.PATH + "/")
					.request()
					.post(Entity.entity(users[i], MediaType.APPLICATION_JSON));

			if(response.getStatus() == 200) {
				System.out.println("Created post " + posts[i].getMessage() + " sucessfully!");
				posts[i].setId(response.readEntity(String.class));
			} else
				System.out.println("Created post " + posts[i].getMessage() + " error: " + response.getStatus());
		}

		for(int i = 0; i < n_likes; i++) {
			String user = users[(int)(Math.random()*n_users)].getName();
			String post = posts[(int)(Math.random()*n_posts)].getId();

			String url = String.format("/%s/like/%s", post, user);

			response = target.path(PostResource.PATH + url)
					.request()
					.post(Entity.entity("", MediaType.APPLICATION_JSON));

			String id = user +  "@" + post;
			if(response.getStatus() == 200) {
				System.out.println("Created like " + id + " sucessfully!");
				posts[i].setId(response.readEntity(String.class));
			} else
				System.out.println("Created like " + id + " error: " + response.getStatus());
		}

		System.out.println("Finished!");
	}

}

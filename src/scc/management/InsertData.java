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
import scc.utils.GSON;

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
			System.out.println("Version: " + response.readEntity(String.class));
		} else
			throw new RuntimeException("Exception: " + response.getStatus());

		Faker faker = new Faker(new Locale("en-US"));

		System.out.println("\nStarting...");

		int n_users = 100;
		int n_communities = 15;
		int n_posts = 500;
		int n_likes = 1200;
		int n_images = 0;

		System.out.println("\n\t Images \n");

		List<String> _images = new ArrayList<>(50);

		File folder = new File("./images");
		int j = 0;
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.isFile()) {
				Path fileLocation = Paths.get(fileEntry.getPath());
				try {
					byte[] data = Files.readAllBytes(fileLocation);

					response = target.path(ImageResource.PATH + "/")
							.request()
							.post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));

					if(response.getStatus() == 200) {
						String id = response.readEntity(String.class);
						System.out.println("(" + j + ") " + "Uploaded image " + fileEntry.getName() + " sucessfully! -> " + id);
						_images.add(id);
					} else
						System.out.println("(" + j + ") " + "Upload image " + fileEntry.getName() + " error: " + response.getStatus() + " " + response.readEntity(String.class));
					j++;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		String[] images = _images.toArray(new String[_images.size()]);

		System.out.println("\n\t Users \n");

		User[] users = new User[n_users];
		for(int i = 0; i < n_users; i++) {
			users[i] = new User(faker.name().username());
			response = target.path(UserResource.PATH + "/")
					.request()
					.post(Entity.entity(GSON.toJson(users[i]), MediaType.APPLICATION_JSON));



			if(response.getStatus() == 200)
				System.out.println("(" + i + ") " + "Created user " + users[i].getName() + " sucessfully! -> " + response.readEntity(String.class));
			else
				System.out.println("(" + i + ") " + "Create user " + users[i].getName() + " error: " + response.getStatus() + " " + response.readEntity(String.class));
		}

		System.out.println("\n\t Communities \n");

		Community[] communities = new Community[n_communities];
		for(int i = 0; i < n_communities; i++) {
			String name = faker.address().country().replace(" ", "_").toLowerCase();
			int ix = name.indexOf("(");
			if(ix > 0)
				name = name.substring(0, ix-1);

			communities[i] = new Community(name);
			response = target.path(CommunityResource.PATH + "/")
					.request()
					.post(Entity.entity(GSON.toJson(communities[i]), MediaType.APPLICATION_JSON));

			if(response.getStatus() == 200)
				System.out.println("(" + i + ") " + "Created community " + communities[i].getName() + " sucessfully! -> " + response.readEntity(String.class));
			else
				System.out.println("(" + i + ") " + "Creat community " + communities[i].getName() + " error: " + response.getStatus() + " " + response.readEntity(String.class));
		}

		System.out.println("\n\t Posts \n");

		Post[] posts = new Post[n_posts];
		for(int i = 0; i < n_posts; i++) {
			double r = Math.random();
			String title = r <= 0.3 ? faker.book().title() : r <= 0.6 ? faker.educator().course() : faker.food().ingredient();
			String author = users[(int)(Math.random()*n_users)].getName();
			String community = communities[(int)(Math.random()*n_communities)].getName();
			String message = Math.random() <= 0.5 ? faker.shakespeare().romeoAndJulietQuote() : faker.chuckNorris().fact();
			String media = Math.random() <= 0.7 || i == 0 ? null : images[(int)(Math.random()*n_images)];
			String parent = Math.random() <= 0.3 || i == 0 ? null : posts[(int)(Math.random()*(i-1))].getId();

			posts[i] = new Post(title, author, community, message, media, parent);
			response = target.path(PostResource.PATH + "/")
					.request()
					.post(Entity.entity(GSON.toJson(posts[i]), MediaType.APPLICATION_JSON));

			if(response.getStatus() == 200) {
				String id = response.readEntity(String.class);
				System.out.println("(" + i + ") " + "Created post " + posts[i].getMessage() + " sucessfully! -> " + id);
				posts[i].setId(id);
			} else
				System.out.println("(" + i + ") " + "Create post " + posts[i].getMessage() + " error: " + response.getStatus() + " " + response.readEntity(String.class));
		}

		System.out.println("\n\t Likes \n");

		for(int i = 0; i < n_likes; i++) {
			String user = users[(int)(Math.random()*n_users)].getName();
			String post = posts[(int)(Math.random()*n_posts)].getId();

			String url = String.format("/%s/like/%s", post, user);

			response = target.path(PostResource.PATH + url)
					.request()
					.post(Entity.entity("", MediaType.APPLICATION_JSON));

			String tag = user +  "@" + post;
			if(response.getStatus() == 200) {
				String id = response.readEntity(String.class);
				System.out.println("(" + i + ") " + "Created like " + tag + " sucessfully! -> " + id);
				posts[i].setId(id);
			} else
				System.out.println("(" + i + ") " + "Create like " + tag + " error: " + response.getStatus() + " " + response.readEntity(String.class));
		}

		System.out.println("\nFinished!");
	}

}

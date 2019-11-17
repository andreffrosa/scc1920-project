package scc.controllers;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.FeedResponse;
import redis.clients.jedis.Jedis;
import scc.models.PostWithReplies;
import scc.storage.CosmosClient;
import scc.storage.Redis;
import scc.utils.GSON;
import scc.utils.MyBase64;

@Path(PagesResource.PATH)
public class PagesResource {

	public static final String PATH = "/page";
	private static final String INITIAL_PAGE = "initial_page";
	private static final int DEFAULT_INITIAL_PAGE_SIZE = 10;
	private static final int DEFAULT_LEVEL = 3;
	private static final int DEFAULT_PAGE_SIZE = 5;
	private static final int MAX_SIZE_ALLOWED = 2;

	@GET
	@Path("/thread/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public PostWithReplies getThread(@PathParam("id") String id, @DefaultValue(""+DEFAULT_LEVEL) @QueryParam("d") int depth, @DefaultValue(""+DEFAULT_PAGE_SIZE) @QueryParam("p") int pageSize, @QueryParam("t") String continuationToken) {

		if(continuationToken != null)
			continuationToken = MyBase64.decode(continuationToken);
		
		PostWithReplies post = CosmosClient.getByIdUnparse(PostResource.CONTAINER, id, PostWithReplies.class);
		if(post == null)
			throw new WebApplicationException( Response.status(Status.NOT_FOUND).entity("Post does not exists").build() );

		Queue<PostWithReplies> queue = new LinkedList<>();
		queue.add(post);
		int current_level = 0, amount_posts_current_level = 1;
		while(!queue.isEmpty()) { 
			PostWithReplies current_post = queue.poll();
			amount_posts_current_level--;

			String query_replies = "SELECT * FROM %s p WHERE p.parent='" + current_post.getId() +"'";
			Entry<String,List<PostWithReplies>> entry = CosmosClient.queryAndUnparsePaginated(PostResource.CONTAINER, query_replies, continuationToken, pageSize, PostWithReplies.class);
			List<PostWithReplies> replies = entry.getValue();
			current_post.setReplies(replies);
			current_post.setContinuationToken(MyBase64.encode(entry.getKey()));

			// TODO: fazer uma lpush com as replies e quando há uma nova reply, adicioná-la a essa lista.
			
			// TODO: Ir buscar à cache os likes
			
			String query_likes = "SELECT COUNT(c) as Likes FROM %s c WHERE c.post_id='" + current_post.getId() +"'";
			List<String> likes = CosmosClient.query(PostResource.LIKE_CONTAINER, query_likes); 
			if(!likes.isEmpty()) {
				JsonElement root = JsonParser.parseString(likes.get(0));
				int n_likes = root.getAsJsonObject().get("Likes").getAsInt();
				current_post.setLikes(n_likes);
			}

			if(current_level < depth) {
				queue.addAll(replies);
			}

			if(amount_posts_current_level == 0) {
				current_level++;
				amount_posts_current_level = queue.size();
			}
		}
		
		

		return post;
	}

	@GET
	@Path("/initial")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PostWithReplies> getInitialPage(@DefaultValue(""+DEFAULT_INITIAL_PAGE_SIZE) @QueryParam("ps") int n_posts, @DefaultValue(""+MAX_SIZE_ALLOWED) @QueryParam("m") int max_size) {
		
		try {
			List<String> fromCache = Redis.getList(INITIAL_PAGE, n_posts);
			if(fromCache!= null && !fromCache.isEmpty()){

				return fromCache.parallelStream().map(d -> GSON.fromJson(d , PostWithReplies.class))
						.collect(Collectors.toList());

			} else {
				Comparator<Entry<Integer, PostWithReplies>> comp = (x, y) -> x.getKey().compareTo(y.getKey());
				Queue<Entry<Integer, PostWithReplies>> queue = new PriorityQueue<Entry<Integer, PostWithReplies>>(n_posts, comp);

				String query = "SELECT * FROM %s p WHERE p.parent=null";
				Iterator<FeedResponse<Document>> it = CosmosClient.queryIterator(PostResource.CONTAINER, query);
				while( it.hasNext() ) {
					Iterable<PostWithReplies> postsWithReplies = it.next().getResults().stream().map((Document d) -> GSON.fromJson(d.toJson(), PostWithReplies.class)).collect(Collectors.toList());
					for (PostWithReplies p : postsWithReplies) {


						// TODO: QUando os likes e replies nas ultimas 24h são 0, o score é 0 e empatam todos e são todos colocados na página inicial
						// -> caso a hotness seja 0, o que fazer? Usar a freshness (quanto menor for a data da criação maior o rating?) também?
						// -> utilizar também o nº de views (guardar na cache) para medir a hotness
						// -> o nº de replies que conta para a hotness deveria ser dos filhos dos filhos ... e não apenas as replies diretas porque se tiver poucas replies directas mas muitas indirectas também deveria aparecer aqui!
						// -> calcular certas coisas (parametros) daqui com maiores intervalos que outras
						// -> Depois de obter a lista da cache, atualizar o nº de likes em direto? (outra leitura à cache para não estar a enviar o nº de likes que existiam quando a paǵina foi calculada.
						//
						// -> usar o estar em cache do post principal para o score também -> trending (?)

						int score = getScore(p);
						if (queue.size() < n_posts) {
							queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(score, p));
						} else {
							Entry<Integer, PostWithReplies> e = queue.peek();
							if (queue.size() >= n_posts) {
								if (e.getKey() < score) {
									queue.poll();
									queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(score, p));
								} else if (e.getKey() == score) {
								/*if(queue.size() < max_size*n_posts)
									queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(score, p));
								else if(queue.size() == max_size*n_posts) {*/
									if (Math.random() <= 0.5) { // Replace with 50% probability
										queue.poll();
										queue.add(new AbstractMap.SimpleEntry<Integer, PostWithReplies>(score, p));
									}
								}
								//}
							}
						}
					}
				}

				List<PostWithReplies> list = queue.stream().map(e -> e.getValue()).collect(Collectors.toList());
				
//				Redis.putInList(INITIAL_PAGE, queue.stream().map(e -> GSON.toJson(e.getValue())).toArray(String[]::new));
//				queue.forEach( e -> Redis.set(GSON.toJson(e.getKey()), GSON.toJson(e.getValue()))); //Inserting rating in the cache
				
				return list;
			}
		} catch(Exception e) {
			throw new WebApplicationException( Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build() );
		}

	}

	private static int getFreshness(PostWithReplies p) {
		double days = ((System.currentTimeMillis() / 1000) - p.getCreationTime() + 1) / ((double)( 24 * 60 * 60 ));
		
		if(days < 0.3)
			days = 0.3;
		//int freshness = Math.min(100, (int)(100.0/(2.0*days)));
		//int freshness = (int)Math.round((100.0/(2.0*days)));
		int freshness = (int)Math.round(100.0/days);
		return freshness;
	}

	private static int getPopularity(PostWithReplies p) {
		// Total Replies
		String query = "SELECT * FROM %s p WHERE p.parent='" + p.getId();
		List<PostWithReplies> replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);
		p.setReplies(replies);

		// Total Likes
		query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='" + p.getId();
		List<String> likes = CosmosClient.query(PostResource.LIKE_CONTAINER, query);
		if (!likes.isEmpty()) {
			JsonElement root = JsonParser.parseString(likes.get(0));
			int n_likes = root.getAsJsonObject().get("Likes").getAsInt();
			p.setLikes(n_likes);
		}

		int n_likes = p.getLikes();
		int n_replies = p.getReplies().size();
		int a = (int) Math.round(0.8 * n_likes + 0.2 * n_replies);
		int b = (int) Math.round(0.2 * n_likes + 0.8 * n_replies);
		int popularity = Math.max(a, b);
		return popularity;
	}

	private static int getHotness(PostWithReplies p) {
		long time = (System.currentTimeMillis() / 1000) - (24 * 60 * 60);

		// Replies in last 24h
		String query = "SELECT * FROM %s p WHERE p.parent='" + p.getId() + "' AND p._ts>=" + time;
		List<PostWithReplies> replies = CosmosClient.queryAndUnparse(PostResource.CONTAINER, query, PostWithReplies.class);
		//p.setReplies(replies);
		int n_replies = replies.size();

		// Likes in last 24h
		query = "SELECT COUNT(l) as Likes FROM %s l WHERE l.post_id='" + p.getId() + "' AND l._ts>=" + time;
		List<String> likes = CosmosClient.query(PostResource.LIKE_CONTAINER, query);

		int n_likes = 0;
		if (!likes.isEmpty()) {
			JsonElement root = JsonParser.parseString(likes.get(0));
			n_likes = root.getAsJsonObject().get("Likes").getAsInt();
			//p.setLikes(n_likes);
		}

		/*int a = (int) Math.round(0.8 * n_likes + 0.2 * n_replies);
		int b = (int) Math.round(0.2 * n_likes + 0.8 * n_replies);
		int c = (int) Math.round(0.5 * n_likes + 0.5 * n_replies);
		int hotness = Math.max(n_likes, Math.max(n_replies, Math.max(a, Math.max(b, c))));*/
		//int hotness = (int) Math.round((100.0 * sigmoid(Math.max(n_likes, n_replies))));
		//int hotness = Math.max(n_likes, n_replies);
		
		int a = (int) Math.round(0.8 * n_likes + 0.2 * n_replies);
		int b = (int) Math.round(0.2 * n_likes + 0.8 * n_replies);
		int hotness = Math.max(a, b);
		
		return hotness;
	}

	private static int getTrending(PostWithReplies p) {
		// TODO: Ir ver se está no Top Posts na cache
		return 0; // se não estiver, 100 se estiver?
	}
	
	private static int getScore(PostWithReplies p) {
		int freshness = getFreshness(p);
		int hotness = getHotness(p);
		int popularity = getPopularity(p);
		int trending = getTrending(p);
		/*int a = (int) Math.round(0.8 * freshness + 0.2 * hotness);
		int b = (int) Math.round(0.2 * freshness + 0.8 * hotness);
		int c = (int) Math.round(0.5 * freshness + 0.5 * hotness);
		int score = Math.max(freshness, Math.max(hotness, Math.max(a, Math.max(b, c))));*/
		int score = (int)Math.round((0.12*popularity + 0.24*freshness + 0.24*trending + 0.4*hotness));
		return score;
	}

	/*public static double sigmoid(double x) { // Converts into range [0;1]
	    return (1.0/( 1.0 + Math.pow(Math.E,(-1.0*x))));
	  }*/

}

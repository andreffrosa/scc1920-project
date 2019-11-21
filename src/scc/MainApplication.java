package scc;

import scc.endpoints.*;

import scc.utils.Config;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class MainApplication extends Application {

	public MainApplication() throws IOException {
		Config.load();
	}
	
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        set.add(DebugEndpoint.class);
        set.add(CommunityEndpoint.class);
        set.add(ImageEndpoint.class);
        set.add(PostEndpoint.class);
        set.add(UserEndpoint.class);
        set.add(PageEndpoint.class);
        return set;
    }
    
}
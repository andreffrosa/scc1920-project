package scc;

import scc.controllers.*;
import scc.storage.Config;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class MainApplication extends Application {

	public MainApplication() {
		Config.load();
	}
	
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        set.add(Debug.class);
        set.add(CommunityResource.class);
        set.add(ImageResource.class);
        set.add(PostResource.class);
        set.add(UserResouce.class);
        return set;

    }
    
}
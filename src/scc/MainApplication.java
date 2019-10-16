package scc;

import scc.storage.config.Config;
import scc.controllers.*;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class MainApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        set.add(Debug.class);
        set.add(CommunityResource.class);
        set.add(ImageResource.class);
        set.add(PostResource.class);
        set.add(UserResouce.class);
        set.add(Config.class);

        return set;

    }
    
}
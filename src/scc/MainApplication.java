package scc;

import scc.controllers.CommunityResource;
import scc.controllers.MediaResource;
import scc.controllers.PostResource;
import scc.controllers.UserResouce;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class MainApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        set.add(CommunityResource.class);
        set.add(MediaResource.class);
        set.add(PostResource.class);
        set.add(UserResouce.class);
        return set;
    }
    
}
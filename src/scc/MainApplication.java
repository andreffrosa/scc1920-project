package scc;

import scc.controllers.CommunityResource;
import scc.controllers.MediaResource;
import scc.models.Community;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class MainApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        set.add(MediaResource.class);
        set.add(CommunityResource.class);
        set.add(Community.class);
        return set;
    }
    
}
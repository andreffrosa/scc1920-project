package scc.models;

public class Resource {

    private String id;
    private Long _ts;

    public Resource() {
        this.id = null;
        this._ts = null;
    }

    public String getId() {
        return id;
    }

    public void setCreationTime(long creation_time){
        this._ts = creation_time;
    }

    public long getCreationTime() {
        return _ts == null ? -1 : _ts.longValue();
    }

}

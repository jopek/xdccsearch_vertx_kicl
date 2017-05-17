package search;

public class ClientSettings {
    private String host;

    private String scheme;

    private int port;

    private String path;

    private String queryParam;

    private String pageParam;

    private boolean usePathForQuery;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getQueryParam() {
        return queryParam;
    }

    public void setQueryParam(String queryParam) {
        this.queryParam = queryParam;
    }

    public boolean isUsePathForQuery() {
        return usePathForQuery;
    }

    public void setUsePathForQuery(boolean usePathForQuery) {
        this.usePathForQuery = usePathForQuery;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPageParam() {
        return pageParam;
    }

    public void setPageParam(String pageParam) {
        this.pageParam = pageParam;
    }

    @Override
    public String toString() {
        return "ClientSettings{" +
                "host='" + host + '\'' +
                ", scheme='" + scheme + '\'' +
                ", port=" + port +
                ", path='" + path + '\'' +
                ", queryParam='" + queryParam + '\'' +
                ", pageParam='" + pageParam + '\'' +
                ", usePathForQuery=" + usePathForQuery +
                '}';
    }
}

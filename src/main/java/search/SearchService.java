package search;

import com.lxbluem.irc.pack.PackInfo;
import dagger.Lazy;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SearchService {
    private Lazy<SearchClient> client;

    @Inject
    public SearchService(Lazy<SearchClient> client) {
        this.client = client;
    }

    /**
     * @param query
     * @return list of packinfo, where nickname is not missing
     */
    public PagedResult<PackInfo> searchFor(String query, int page) throws IOException {
        PagedResult<PackInfo> search = client.get().search(query, page);
        List<PackInfo> collect = search.getResults().stream()
                .filter((packInfo) -> (!packInfo.isNickNameMissing()))
                .collect(Collectors.toList());
        search.getResults().clear();
        search.getResults().addAll(collect);
        return search;
    }
}

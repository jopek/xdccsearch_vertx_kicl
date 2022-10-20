package com.lxbluem.search.domain.ports;

import com.lxbluem.search.domain.callback.Callback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

public interface SearchGateway {
    void doSearch(SearchQuery query, Callback<SearchResponse> consumer);

    @Getter
    @AllArgsConstructor
    class SearchQuery {
        private final String searchTerm;
        private final int pageNumber;
    }

    @Getter
    @Builder
    @EqualsAndHashCode
    @ToString
    class SearchResponse {
        List<ResponsePack> results;
        int currentPage;
        boolean hasMore;
    }

    @Getter
    @Builder
    @EqualsAndHashCode
    @ToString
    class ResponsePack {
        private final String networkName;
        private final String serverHostName;
        private final int serverPort;
        private final String channelName;
        private final String nickName;
        private final int packNumber;
        private final String packName;
        private final long sizeBytes;
        private final int packGets;
        private final int age;
        private final int last;
    }

}

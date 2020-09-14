package com.lxbluem.search.domain.ports;

import com.lxbluem.search.domain.callback.Callback;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

public interface ListMatchingPacks {
    void handle(Command command, Callback<ListMatchingPacksResponse> presenter);

    @Getter
    @AllArgsConstructor
    class Command {
        private final String searchTerm;
        private final int pageNumber;
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString
    class ListMatchingPacksResponse extends SearchGateway.SearchResponse {
        public ListMatchingPacksResponse(SearchGateway.SearchResponse searchResponse) {
            super(searchResponse.getResults(), searchResponse.getCurrentPage(), searchResponse.isHasMore());
        }

        public ListMatchingPacksResponse(
                List<SearchGateway.ResponsePack> results,
                int currentPage,
                boolean hasMore
        ) {
            super(results, currentPage, hasMore);
        }
    }
}

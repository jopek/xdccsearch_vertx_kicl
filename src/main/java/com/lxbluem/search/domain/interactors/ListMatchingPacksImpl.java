package com.lxbluem.search.domain.interactors;

import com.lxbluem.search.domain.callback.Callback;
import com.lxbluem.search.domain.ports.ListMatchingPacks;
import com.lxbluem.search.domain.ports.SearchGateway;

public class ListMatchingPacksImpl implements ListMatchingPacks {
    private final SearchGateway searchGateway;

    public ListMatchingPacksImpl(SearchGateway searchGateway) {
        this.searchGateway = searchGateway;
    }

    @Override
    public void handle(Command command, Callback<ListMatchingPacksResponse> presenter) {
        int pageNumber = command.getPageNumber();
        String searchTerm = command.getSearchTerm();

        if (searchTerm.isEmpty()) {
            String message = "please provide a searchTerm for jsonobject key q";
            RuntimeException throwable = new RuntimeException(message);
            presenter.failure(throwable);
            return;
        }

        SearchGateway.SearchQuery searchQuery = new SearchGateway.SearchQuery(searchTerm, pageNumber);

        Callback<SearchGateway.SearchResponse> result = Callback.of(
                searchResponse -> presenter.success(new ListMatchingPacksResponse(searchResponse)),
                presenter::failure
        );

        searchGateway.doSearch(searchQuery, result);
    }

}

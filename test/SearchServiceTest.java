package com.example.searchservice;

import com.azure.core.util.paging.PagedResponseBase;
import com.azure.search.documents.SearchAsyncClient;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchPagedFlux;
import com.azure.search.documents.models.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SearchServiceTest {

    private SearchAsyncClient searchAsyncClient;
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchAsyncClient = mock(SearchAsyncClient.class);
        searchService = new SearchService(searchAsyncClient);
    }

    @Test
    void testSearchFound() {
        // Mock response
        SearchResult result = new SearchResult()
                .setDocument(Map.of("lookupValue", "FoundValue"));

        SearchPagedFlux pagedFlux = new SearchPagedFlux(() ->
                Mono.just(new PagedResponseBase<>(
                        null, 200, null, Collections.singletonList(result), null, null))
        );

        ArgumentCaptor<SearchOptions> optionsCaptor = ArgumentCaptor.forClass(SearchOptions.class);
        when(searchAsyncClient.search(anyString(), optionsCaptor.capture())).thenReturn(pagedFlux);

        StepVerifier.create(searchService.search("testParam", "Bearer token"))
                .expectNextMatches(r -> r.getBody() != null &&
                        r.getBody().getParam1().equals("testParam") &&
                        r.getBody().getLookupValue().equals("FoundValue"))
                .verifyComplete();

        // Verify filter applied correctly
        SearchOptions usedOptions = optionsCaptor.getValue();
        assertThat(usedOptions.getFilter()).isEqualTo("param1Attribute eq 'testParam'");
        assertThat(usedOptions.getTop()).isEqualTo(1);
    }

    @Test
    void testSearchNotFound() {
        SearchPagedFlux pagedFlux = new SearchPagedFlux(() ->
                Mono.just(new PagedResponseBase<>(
                        null, 200, null, Collections.emptyList(), null, null))
        );

        when(searchAsyncClient.search(anyString(), any())).thenReturn(pagedFlux);

        StepVerifier.create(searchService.search("missing", "Bearer token"))
                .expectNextMatches(r -> r.getStatusCode().value() == 204)
                .verifyComplete();
    }

    @Test
    void testSearchError() {
        when(searchAsyncClient.search(anyString(), any()))
                .thenThrow(new RuntimeException("Azure failure"));

        StepVerifier.create(searchService.search("error", "Bearer token"))
                .expectNextMatches(r -> r.getStatusCode().value() == 500)
                .verifyComplete();
    }
}

package com.example.searchservice;

import com.azure.search.documents.SearchAsyncClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchPagedFlux;
import com.azure.search.documents.models.SearchResult;
import com.azure.core.credential.AzureKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public Mono<ResponseEntity<SearchResponse>> search(
            @RequestParam String param1,
            @RequestHeader("Authorization") String authHeader) {
        return searchService.search(param1, authHeader);
    }
}


   @Service
class SearchService {

    private final SearchAsyncClient searchAsyncClient;

    public SearchService(
            @Value("${azure.search.endpoint}") String endpoint,
            @Value("${azure.search.indexName}") String indexName,
            @Value("${azure.search.apiKey}") String apiKey) {

        this.searchAsyncClient = new SearchClientBuilder()
                .endpoint(endpoint)
                .indexName(indexName)
                .credential(new AzureKeyCredential(apiKey))
                .buildAsyncClient();
    }

    public Mono<ResponseEntity<SearchResponse>> search(String param1, String authHeader) {
        // Exact match filter on param1Attribute field in index
        SearchOptions options = new SearchOptions()
                .setFilter(String.format("param1Attribute eq '%s'", param1))
                .setTop(1);

        // Empty string search text to only rely on filter
        SearchPagedFlux results = searchAsyncClient.search("*", options);

        return results
                .byPage()
                .next()
                .flatMap(page -> {
                    if (page.getValue().isEmpty()) {
                        return Mono.just(ResponseEntity.noContent().build());
                    }
                    SearchResult result = page.getValue().get(0);
                    Map<String, Object> doc = result.getDocument(Map.class);

                    SearchResponse response = new SearchResponse(
                            param1,
                            doc.getOrDefault("lookupValue", "").toString()
                    );
                    return Mono.just(ResponseEntity.ok(response));
                })
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                );
    }
}
 

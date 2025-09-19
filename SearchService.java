@Service
class SearchService {

    private final SearchAsyncClient searchAsyncClient;

    // Inject client for easier testing
    public SearchService(SearchAsyncClient searchAsyncClient) {
        this.searchAsyncClient = searchAsyncClient;
    }

    public Mono<ResponseEntity<SearchResponse>> search(String param1, String authHeader) {
        // Exact match filter
        String filter = String.format("param1Attribute eq '%s'", param1);

        SearchOptions options = new SearchOptions()
                .setFilter(filter)
                .setTop(1);

        // Use "*" search text with filter
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

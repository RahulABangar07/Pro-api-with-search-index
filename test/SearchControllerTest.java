@WebFluxTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private SearchService searchService;

    @Test
    void testControllerFound() {
        when(searchService.search(anyString(), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok(new SearchResponse("testParam", "FoundValue"))));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/search")
                        .queryParam("param1", "testParam")
                        .build())
                .header("Authorization", "Bearer token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.param1").isEqualTo("testParam")
                .jsonPath("$.lookupValue").isEqualTo("FoundValue");
    }

    @Test
    void testControllerNoContent() {
        when(searchService.search(anyString(), anyString()))
                .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).build()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/search")
                        .queryParam("param1", "missing")
                        .build())
                .header("Authorization", "Bearer token")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void testControllerError() {
        when(searchService.search(anyString(), anyString()))
                .thenReturn(Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/search")
                        .queryParam("param1", "error")
                        .build())
                .header("Authorization", "Bearer token")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}

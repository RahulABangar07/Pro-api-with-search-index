package com.example.searchservice;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

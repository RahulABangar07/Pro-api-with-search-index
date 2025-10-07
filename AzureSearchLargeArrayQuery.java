import com.azure.core.util.paging.ContinuablePagedFlux;
import com.azure.search.documents.SearchAsyncClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AzureSearchLargeArrayQuery {

    private static final String ENDPOINT = "<your-search-service-endpoint>";
    private static final String API_KEY = "<your-api-key>";
    private static final String INDEX_NAME = "<your-index-name>";

    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        List<String> inputTags = loadLargeTagList(); // Your input data

        SearchAsyncClient client = new SearchClientBuilder()
                .endpoint(ENDPOINT)
                .credential(new AzureKeyCredential(API_KEY))
                .indexName(INDEX_NAME)
                .buildAsyncClient();

        List<List<String>> chunks = chunkList(inputTags, BATCH_SIZE);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        List<CompletableFuture<List<SearchResult>>> futures = new ArrayList<>();

        for (List<String> chunk : chunks) {
            CompletableFuture<List<SearchResult>> future = CompletableFuture.supplyAsync(() -> {
                String filter = buildFilter(chunk);
                SearchOptions options = new SearchOptions().setFilter(filter);
                ContinuablePagedFlux<Void, SearchResult> results = client.search("*", options);

                List<SearchResult> collected = new ArrayList<>();
                results.byPage().toIterable().forEach(page -> collected.addAll(page.getValue()));

                return collected;
            }, executor);

            futures.add(future);
        }

        // Wait for all futures
        List<SearchResult> allResults = new ArrayList<>();
        for (CompletableFuture<List<SearchResult>> future : futures) {
            allResults.addAll(future.get());
        }

        // Optional: deduplicate by document ID
        Map<String, SearchResult> deduped = allResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getDocument().get("id").toString(),
                        result -> result,
                        (r1, r2) -> r1
                ));

        System.out.println("Total unique results: " + deduped.size());
        executor.shutdown();
    }

    // Build filter like: tags/any(t: t eq 'tag1' or t eq 'tag2' ...)
    private static String buildFilter(List<String> values) {
        return "tags/any(t: " +
                values.stream()
                        .map(val -> "t eq '" + val.replace("'", "''") + "'")
                        .collect(Collectors.joining(" or ")) +
                ")";
    }

    // Chunk large list into batches
    private static <T> List<List<T>> chunkList(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }

    private static List<String> loadLargeTagList() {
        // Replace with your actual tag list loading logic
        return Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5", "tag6");
    }
}

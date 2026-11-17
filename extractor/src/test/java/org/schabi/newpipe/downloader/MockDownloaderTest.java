package org.schabi.newpipe.downloader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class MockDownloaderTest {
    @TempDir
    Path directory;

    @Test
    void repeatedRequestsUseFirstRecordingInNumericOrder() throws Exception {
        final Request request = Request.newBuilder().get("https://example.com/page").build();
        record(10, request, "later page");
        record(2, request, "first page");
        final MockDownloader downloader = new MockDownloader(directory.toString());
        assertEquals("first page", downloader.execute(request).responseBody());
        assertEquals("first page", downloader.execute(request).responseBody());
    }

    @Test
    void differentContinuationBodiesStillSelectDifferentResponses() throws Exception {
        final Request first = Request.newBuilder().post("https://example.com/page", new byte[] {1})
                .build();
        final Request next = Request.newBuilder().post("https://example.com/page", new byte[] {2})
                .build();
        record(1, next, "next page");
        record(0, first, "first page");
        final MockDownloader downloader = new MockDownloader(directory.toString());
        assertEquals("first page", downloader.execute(first).responseBody());
        assertEquals("next page", downloader.execute(next).responseBody());
    }

    private void record(final int index, final Request request, final String body) throws Exception {
        Files.writeString(directory.resolve("generated_mock_" + index + ".json"), new Gson().toJson(
                new TestRequestResponse(request, new Response(200, "OK", Map.of(), body,
                        request.url()))));
    }
}

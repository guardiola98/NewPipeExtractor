package org.schabi.newpipe.downloader;

import com.google.gson.GsonBuilder;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * <p>
 * Mocks requests by using json files created by {@link RecordingDownloader}
 * </p>
 */
class MockDownloader extends Downloader {

    private final String path;
    private final Map<Request, Response> mocks;

    public MockDownloader(@Nonnull final String path) {
        this.path = path;
        this.mocks = new HashMap<>();

        try (final var directoryStream = Files.newDirectoryStream(Paths.get(path),
                entry -> entry.getFileName().toString()
                        .startsWith(RecordingDownloader.FILE_NAME_PREFIX))) {
            final List<Path> entries = new ArrayList<>();
            directoryStream.forEach(entries::add);
            entries.sort(Comparator.comparingInt(entry -> {
                final String name = entry.getFileName().toString();
                return Integer.parseInt(name.substring(RecordingDownloader.FILE_NAME_PREFIX.length(),
                        name.lastIndexOf('.')));
            }));
            for (final Path entry : entries) {
                try (final var reader = Files.newBufferedReader(entry)) {
                    final var response = new GsonBuilder()
                            .create()
                            .fromJson(reader, TestRequestResponse.class);
                    // Repeated requests can have different live responses. Keep the first
                    // recording so pagination does not depend on filesystem iteration order.
                    mocks.putIfAbsent(response.getRequest(), response.getResponse());
                }
            }
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Override
    public Response execute(@Nonnull final Request request) {
        final Response result = mocks.get(request);
        if (result == null) {
            throw new NullPointerException("No mock response for request with url '" + request
                    .url() + "' exists in path '" + path + "'.\nPlease make sure to run the tests "
                    + "with the RecordingDownloader first after changes.");
        }
        return result;
    }
}

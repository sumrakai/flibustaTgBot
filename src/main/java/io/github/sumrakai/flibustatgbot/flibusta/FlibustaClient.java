package io.github.sumrakai.flibustatgbot.flibusta;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

@Component
public class FlibustaClient {

    private static final String BASE_URL = "https://flibusta.is";
    private static final String SEARCH_PATH = "/booksearch";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private static final int SEARCH_TIMEOUT_MILLIS = (int) Duration.ofSeconds(10).toMillis();
    private static final int DOWNLOAD_TIMEOUT_MILLIS = (int) Duration.ofSeconds(60).toMillis();
    private static final long REQUEST_DELAY_MILLIS = 800L;

    private long lastRequestAt = 0L;

    private final FlibustaParser parser;

    public FlibustaClient(FlibustaParser parser) {
        this.parser = parser;
    }

    public List<Book> search(String query, int limit) throws IOException {
        throttle();

        Document doc = Jsoup
                .connect(BASE_URL + SEARCH_PATH)
                .userAgent(USER_AGENT)
                .timeout(SEARCH_TIMEOUT_MILLIS)
                .data("ask", query)
                .get();

        return parser.parseSearchResults(doc, limit);
    }

    public String buildDownloadUrl(String bookId, String format) {
        return BASE_URL + "/b/" + bookId + "/" + format.toLowerCase();
    }

    public long getFileSize(String bookId, String format) throws IOException {
        throttle();

        String url = buildDownloadUrl(bookId, format);
        Connection.Response response = Jsoup
                .connect(url)
                .userAgent(USER_AGENT)
                .timeout(DOWNLOAD_TIMEOUT_MILLIS)
                .ignoreContentType(true)
                .method(Connection.Method.HEAD)
                .execute();

        String lengthHeader = response.header("Content-Length");
        if (lengthHeader == null) {
            return -1L;
        }
        try {
            return Long.parseLong(lengthHeader);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    public Path downloadBook(String bookId, String format) throws IOException {
        throttle();

        String url = buildDownloadUrl(bookId, format);
        Connection.Response response = Jsoup
                .connect(url)
                .userAgent(USER_AGENT)
                .timeout(DOWNLOAD_TIMEOUT_MILLIS)
                .ignoreContentType(true)
                .maxBodySize(0)
                .execute();

        Path tempFile = Files.createTempFile("flibusta_", "." + format.toLowerCase());

        try (InputStream in = response.bodyStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }

    private synchronized void throttle() {
        long now = System.currentTimeMillis();
        long sinceLast = now - lastRequestAt;
        if (sinceLast < REQUEST_DELAY_MILLIS) {
            try {
                Thread.sleep(REQUEST_DELAY_MILLIS - sinceLast);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }
}



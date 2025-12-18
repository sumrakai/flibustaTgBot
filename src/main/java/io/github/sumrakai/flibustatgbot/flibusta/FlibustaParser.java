package io.github.sumrakai.flibustatgbot.flibusta;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FlibustaParser {

    public List<Book> parseSearchResults(Document document, int limit) {
        List<Book> result = new ArrayList<>();

        // Страница flibusta менялась несколько раз, поэтому парсим максимально осторожно
        Elements items = document.select("li:has(a[href^=\"/b/\"])");

        for (Element item : items) {
            Element bookLink = item.selectFirst("a[href^=\"/b/\"]");
            if (bookLink == null) {
                continue;
            }

            String href = bookLink.attr("href"); // /b/123456
            String id = extractBookId(href);
            if (id == null) {
                continue;
            }

            String title = bookLink.text();

            Element authorLink = item.selectFirst("a[href^=\"/a/\"]");
            String author = authorLink != null ? authorLink.text() : "Неизвестный автор";

            result.add(new Book(id, title, author));

            if (result.size() >= limit) {
                break;
            }
        }

        return result;
    }

    private String extractBookId(String href) {
        // ожидаем /b/{id} или /b/{id}/...
        if (href == null || !href.startsWith("/b/")) {
            return null;
        }
        String withoutPrefix = href.substring(3); // после "/b/"
        int slash = withoutPrefix.indexOf('/');
        return slash >= 0 ? withoutPrefix.substring(0, slash) : withoutPrefix;
    }
}



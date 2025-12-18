package io.github.sumrakai.flibustatgbot.flibusta;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlibustaParserTest {

    private final FlibustaParser parser = new FlibustaParser();

    @Test
    void parsesBookListWithAuthorAndId() {
        String html = """
                <html>
                  <body>
                    <ul>
                      <li>
                        <a href="/b/12345">Книга 1</a>
                        &nbsp;/
                        <a href="/a/111">Автор 1</a>
                      </li>
                      <li>
                        <a href="/b/67890/">Книга 2</a>
                        &nbsp;/
                        <a href="/a/222">Автор 2</a>
                      </li>
                    </ul>
                  </body>
                </html>
                """;

        Document document = Jsoup.parse(html);

        List<Book> books = parser.parseSearchResults(document, 10);

        assertThat(books).hasSize(2);

        Book first = books.getFirst();
        assertThat(first.id()).isEqualTo("12345");
        assertThat(first.title()).isEqualTo("Книга 1");
        assertThat(first.author()).isEqualTo("Автор 1");

        Book second = books.get(1);
        assertThat(second.id()).isEqualTo("67890");
        assertThat(second.title()).isEqualTo("Книга 2");
        assertThat(second.author()).isEqualTo("Автор 2");
    }

    @Test
    void fallsBackToUnknownAuthorIfAbsent() {
        String html = """
                <html>
                  <body>
                    <ul>
                      <li>
                        <a href="/b/42">Без автора</a>
                      </li>
                    </ul>
                  </body>
                </html>
                """;

        Document document = Jsoup.parse(html);

        List<Book> books = parser.parseSearchResults(document, 10);

        assertThat(books).hasSize(1);
        Book book = books.getFirst();
        assertThat(book.id()).isEqualTo("42");
        assertThat(book.title()).isEqualTo("Без автора");
        assertThat(book.author()).isEqualTo("Неизвестный автор");
    }

    @Test
    void respectsLimit() {
        String html = """
                <html>
                  <body>
                    <ul>
                      <li><a href="/b/1">Книга 1</a></li>
                      <li><a href="/b/2">Книга 2</a></li>
                      <li><a href="/b/3">Книга 3</a></li>
                    </ul>
                  </body>
                </html>
                """;

        Document document = Jsoup.parse(html);

        List<Book> books = parser.parseSearchResults(document, 2);

        assertThat(books).hasSize(2);
        assertThat(books.get(0).id()).isEqualTo("1");
        assertThat(books.get(1).id()).isEqualTo("2");
    }
}



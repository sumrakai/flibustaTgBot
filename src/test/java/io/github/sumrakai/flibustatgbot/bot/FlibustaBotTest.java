package io.github.sumrakai.flibustatgbot.bot;

import io.github.sumrakai.flibustatgbot.config.BotConfig;
import io.github.sumrakai.flibustatgbot.flibusta.Book;
import io.github.sumrakai.flibustatgbot.flibusta.FlibustaClient;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlibustaBotTest {

    @Test
    void handlesStartCommand() {
        BotConfig config = new BotConfig();
        config.setUsername("test-bot");
        config.setToken("dummy");

        FlibustaClient client = mock(FlibustaClient.class);

        TestBot bot = new TestBot(config, client);

        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(1L);
        message.setChat(chat);
        message.setText("/start");
        update.setMessage(message);

        bot.onUpdateReceived(update);

        assertThat(bot.sentTexts).hasSize(1);
        assertThat(bot.sentTexts.getFirst().chatId).isEqualTo(1L);
        assertThat(bot.sentTexts.getFirst().text).contains("Привет.");

        verifyNoInteractions(client);
    }

    @Test
    void searchesBooksOnTextAndSendsKeyboard() throws Exception {
        BotConfig config = new BotConfig();
        config.setUsername("test-bot");
        config.setToken("dummy");

        FlibustaClient client = mock(FlibustaClient.class);

        List<Book> books = List.of(
                new Book("1", "Книга 1", "Автор 1"),
                new Book("2", "Книга 2", "Автор 2")
        );
        when(client.search(eq("test"), anyInt())).thenReturn(books);

        TestBot bot = new TestBot(config, client);

        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(2L);
        message.setChat(chat);
        message.setText("test");
        update.setMessage(message);

        bot.onUpdateReceived(update);

        assertThat(bot.sentBooks).hasSize(2);
        assertThat(bot.sentBooks.getFirst().chatId).isEqualTo(2L);
        assertThat(bot.sentBooks.get(0).bookIndex).isEqualTo(0);
        assertThat(bot.sentBooks.get(0).text).contains("Книга 1");
        assertThat(bot.sentBooks.get(1).bookIndex).isEqualTo(1);
    }

    @Test
    void downloadsBookOnCallbackAndSendsDocument() throws Exception {
        BotConfig config = new BotConfig();
        config.setUsername("test-bot");
        config.setToken("dummy");

        FlibustaClient client = mock(FlibustaClient.class);
        TestBot bot = new TestBot(config, client);

        // сначала имитируем поиск, чтобы в памяти лежали результаты
        List<Book> books = List.of(new Book("10", "Книга 10", "Автор 10"));
        when(client.search(eq("query"), anyInt())).thenReturn(books);

        Update searchUpdate = new Update();
        Message searchMessage = new Message();
        Chat chat = new Chat();
        chat.setId(3L);
        searchMessage.setChat(chat);
        searchMessage.setText("query");
        searchUpdate.setMessage(searchMessage);

        bot.onUpdateReceived(searchUpdate);

        // теперь колбэк по первой книге и формату fb2
        when(client.getFileSize("10", "fb2")).thenReturn(1024L);

        Path tmp = Files.createTempFile("test_book_", ".fb2");
        when(client.downloadBook("10", "fb2")).thenReturn(tmp);

        Update cbUpdate = new Update();
        CallbackQuery callback = new CallbackQuery();
        Message cbMessage = new Message();
        cbMessage.setChat(chat);
        callback.setMessage(cbMessage);
        callback.setData("0:fb2");
        cbUpdate.setCallbackQuery(callback);

        bot.onUpdateReceived(cbUpdate);

        assertThat(bot.sentDocuments).hasSize(1);
        var sent = bot.sentDocuments.getFirst();
        assertThat(sent.chatId).isEqualTo(3L);
        assertThat(sent.caption).contains("Книга 10");
        assertThat(sent.caption).contains("FB2");
    }

    private static class TestBot extends FlibustaBot {
        record SentText(Long chatId, String text) {
        }

        record SentBook(Long chatId, String text, int bookIndex) {
        }

        record SentDocument(Long chatId, Path filePath, String caption) {
        }

        final List<SentText> sentTexts = new ArrayList<>();
        final List<SentBook> sentBooks = new ArrayList<>();
        final List<SentDocument> sentDocuments = new ArrayList<>();

        TestBot(BotConfig botConfig, FlibustaClient flibustaClient) {
            super(botConfig, flibustaClient);
        }

        @Override
        protected void sendText(Long chatId, String text) {
            sentTexts.add(new SentText(chatId, text));
        }

        @Override
        protected void sendBookWithKeyboard(Long chatId, String text, int bookIndex) {
            sentBooks.add(new SentBook(chatId, text, bookIndex));
        }

        @Override
        protected void sendDocument(Long chatId, Path filePath, String caption) {
            sentDocuments.add(new SentDocument(chatId, filePath, caption));
        }
    }
}



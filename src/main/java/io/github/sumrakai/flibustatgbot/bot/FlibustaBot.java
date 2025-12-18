package io.github.sumrakai.flibustatgbot.bot;

import io.github.sumrakai.flibustatgbot.config.BotConfig;
import io.github.sumrakai.flibustatgbot.flibusta.Book;
import io.github.sumrakai.flibustatgbot.flibusta.FlibustaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FlibustaBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(FlibustaBot.class);

    private static final int MAX_RESULTS = 7;
    private static final long MAX_FILE_SIZE_BYTES = 45L * 1024 * 1024;

    private final BotConfig botConfig;
    private final FlibustaClient flibustaClient;

    private final Map<Long, List<Book>> lastSearchResults = new ConcurrentHashMap<>();

    public FlibustaBot(BotConfig botConfig, FlibustaClient flibustaClient) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.flibustaClient = flibustaClient;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallback(update);
            }
        } catch (Exception e) {
            log.error("Ошибка обработки апдейта", e);
        }
    }

    private void handleTextMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        if ("/start".equals(text)) {
            sendText(chatId,
                    """
                            Привет.
                            Отправь мне название книги или автора — я поищу на flibusta.is.
                            Затем выбери формат (FB2/EPUB/MOBI) под нужной книгой.""");
            return;
        }

        if (text.isEmpty()) {
            sendText(chatId, "Пожалуйста, отправь текст запроса.");
            return;
        }

        try {
            List<Book> books = flibustaClient.search(text, MAX_RESULTS);
            if (books.isEmpty()) {
                sendText(chatId, "Ничего не нашлось.");
                return;
            }

            lastSearchResults.put(chatId, books);

            for (int i = 0; i < books.size(); i++) {
                Book book = books.get(i);
                String messageText = "%d. %s\n%s".formatted(i + 1, book.title(), book.author());
                sendBookWithKeyboard(chatId, messageText, i);
            }
        } catch (IOException e) {
            log.warn("Ошибка при запросе к flibusta", e);
            sendText(chatId, "Не удалось обратиться к flibusta.is. Попробуй позже.");
        }
    }

    private void handleCallback(Update update) {
        var callback = update.getCallbackQuery();
        Long chatId = callback.getMessage().getChatId();
        String data = callback.getData(); // формат: index:format

        String[] parts = data.split(":", 2);
        if (parts.length != 2) {
            sendText(chatId, "Некорректные данные кнопки.");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            sendText(chatId, "Некорректные данные кнопки.");
            return;
        }

        String format = parts[1].toLowerCase();

        List<Book> books = lastSearchResults.get(chatId);
        if (books == null || index < 0 || index >= books.size()) {
            sendText(chatId, "Старый результат поиска. Отправь запрос ещё раз.");
            return;
        }

        Book book = books.get(index);
        String bookId = book.id();

        try {
            long size = flibustaClient.getFileSize(bookId, format);
            String url = flibustaClient.buildDownloadUrl(bookId, format);

            if (size < 0 || size > MAX_FILE_SIZE_BYTES) {
                sendText(chatId, "Файл слишком большой для отправки через Telegram.\n" + url);
                return;
            }

            Path filePath = flibustaClient.downloadBook(bookId, format);
            try {
                sendDocument(chatId, filePath, "%s — %s (%s)".formatted(book.title(), book.author(), format.toUpperCase()));
            } finally {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    log.debug("Не удалось удалить временный файл {}", filePath, e);
                }
            }
        } catch (IOException e) {
            log.warn("Ошибка при загрузке файла для книги {}", bookId, e);
            String url = flibustaClient.buildDownloadUrl(bookId, format);
            sendText(chatId, "Не удалось скачать файл, вот прямая ссылка:\n" + url);
        }
    }

    protected void sendBookWithKeyboard(Long chatId, String text, int bookIndex) {
        SendMessage message = new SendMessage(chatId.toString(), text);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(formatButton("FB2", bookIndex, "fb2")));
        rows.add(List.of(formatButton("EPUB", bookIndex, "epub")));
        rows.add(List.of(formatButton("MOBI", bookIndex, "mobi")));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (Exception e) {
            log.error("Ошибка отправки сообщения с клавиатурой", e);
        }
    }

    private InlineKeyboardButton formatButton(String label, int bookIndex, String format) {
        InlineKeyboardButton button = new InlineKeyboardButton(label);
        button.setCallbackData(bookIndex + ":" + format);
        return button;
    }

    protected void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        try {
            execute(message);
        } catch (Exception e) {
            log.error("Ошибка отправки сообщения", e);
        }
    }

    protected void sendDocument(Long chatId, Path filePath, String caption) {
        SendDocument document = new SendDocument();
        document.setChatId(chatId.toString());
        document.setDocument(new InputFile(filePath.toFile(), filePath.getFileName().toString()));
        document.setCaption(caption);

        try {
            execute(document);
        } catch (Exception e) {
            log.error("Ошибка отправки документа", e);
        }
    }
}



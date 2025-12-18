## Flibusta Telegram Bot

Простой Telegram‑бот на Java 21 / Spring Boot 3, который ищет книги на `flibusta.is` и отдаёт их в форматах FB2/EPUB/MOBI.

### Запуск

Собрать:

```bash
./gradlew clean build
```

Запустить:

```bash
java -jar build/libs/flibustaTgBot-0.0.1-SNAPSHOT.jar
```

В `application.yml` прописать свои данные бота:

```yaml
telegram:
  bot:
    username: YOUR_BOT_USERNAME
    token: YOUR_BOT_TOKEN
```

После старта бота:

- `/start` — краткая инструкция;
- любой текст — поиск на flibusta и список до 7 книг с кнопками FB2/EPUB/MOBI.

### Нюансы с сетью

- `flibusta.is` часто недоступен напрямую (блокировки, DPI).
- Если в логах/через `curl https://flibusta.is` видишь `Connection reset` или таймауты — запускай бота за VPN/прокси на этой же машине.



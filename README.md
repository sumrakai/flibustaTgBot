## Flibusta Telegram Bot

Simple Telegram bot written in Java 21 / Spring Boot 3.  
It searches books on `flibusta.is` and sends them in FB2/EPUB/MOBI formats.

### How to run

Build:

```bash
./gradlew clean build
```

Run:

```bash
java -jar build/libs/flibustaTgBot-0.0.1-SNAPSHOT.jar
```

Configure your bot credentials in `application.yml`:

```yaml
telegram:
  bot:
    username: YOUR_BOT_USERNAME
    token: YOUR_BOT_TOKEN
```

After the bot is started:

- `/start` — short help;
- any text — search on Flibusta and a list of up to 7 books with FB2/EPUB/MOBI buttons.

### Network notes

- `https://flibusta.is` is often blocked or filtered (DPI, ISP blocking).
- If you see `Connection reset` or timeouts in logs / `curl https://flibusta.is`, run the bot behind a VPN/proxy on the same machine.

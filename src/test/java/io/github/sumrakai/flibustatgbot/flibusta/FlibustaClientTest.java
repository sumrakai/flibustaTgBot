package io.github.sumrakai.flibustatgbot.flibusta;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlibustaClientTest {

    @Test
    void buildsDownloadUrlCorrectly() {
        FlibustaClient client = new FlibustaClient(new FlibustaParser());

        String urlFb2 = client.buildDownloadUrl("12345", "fb2");
        String urlEpub = client.buildDownloadUrl("67890", "EPUB");

        assertThat(urlFb2).isEqualTo("https://flibusta.is/b/12345/fb2");
        assertThat(urlEpub).isEqualTo("https://flibusta.is/b/67890/epub");
    }
}



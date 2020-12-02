package com.github.hisener.spring.webclient;

import java.io.IOException;
import java.net.URI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

@Tag("unit")
final class WebClientTest {
    private final MockWebServer server = new MockWebServer();

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    void clientGet() {
        URI requestUrl = server.url("").uri();
        WebClient client = WebClient.create(requestUrl.toString());

        server.enqueue(new MockResponse());
        client.get().retrieve().toBodilessEntity().then().as(StepVerifier::create).verifyComplete();
    }
}

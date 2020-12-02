package com.github.hisener.spring.webclient;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.io.IOException;
import java.net.URI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

@Tag("unit")
@TestInstance(PER_CLASS)
final class WebClientTest {
    private final MockWebServer server = new MockWebServer();

    @AfterAll
    void tearDown() throws IOException {
        server.close();
    }

    @RepeatedTest(100)
    void clientGet() {
        URI requestUrl = server.url("").uri();
        WebClient client = WebClient.create(requestUrl.toString());

        server.enqueue(new MockResponse());
        client.get().retrieve().toBodilessEntity().then().as(StepVerifier::create).verifyComplete();
    }
}

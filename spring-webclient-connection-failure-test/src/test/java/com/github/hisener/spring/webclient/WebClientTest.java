package com.github.hisener.spring.webclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.io.IOException;
import java.net.URI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
    private final CloseableHttpClient apacheClient = HttpClients.createDefault();

    @AfterAll
    void tearDown() throws IOException {
        server.close();
    }

    @RepeatedTest(10)
    void apacheClient() throws IOException {
        URI requestUrl = server.url("").uri();
        server.enqueue(new MockResponse());

        assertThat(apacheClient.execute(new HttpGet(requestUrl)).getStatusLine().getStatusCode())
                .isEqualTo(HttpStatus.SC_OK);
    }

    @RepeatedTest(10)
    void webClientGet() {
        URI requestUrl = server.url("").uri();
        WebClient client = WebClient.create(requestUrl.toString());

        server.enqueue(new MockResponse());
        client.get().retrieve().toBodilessEntity().then().as(StepVerifier::create).verifyComplete();
    }
}

package com.github.hisener.spring.webclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

@Tag("unit")
@TestInstance(PER_CLASS)
final class WebClientTest {
    private final MockWebServer server = new MockWebServer();
    private final URI baseUrl = server.url("").uri();
    private final HttpClient apacheClient = HttpClients.createDefault();

    @BeforeAll
    void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));
    }

    @AfterAll
    void tearDown() throws IOException {
        StepVerifier.resetDefaultTimeout();
        server.close();
    }

    @RepeatedTest(10)
    void apacheClient() throws IOException {
        server.enqueue(new MockResponse());

        assertThat(apacheClient.execute(new HttpGet(baseUrl)).getStatusLine().getStatusCode())
                .isEqualTo(HttpStatus.SC_OK);
    }

    @RepeatedTest(10)
    void reactorNetty() {
        server.enqueue(new MockResponse());

        reactor.netty.http.client.HttpClient.create()
                .baseUrl(baseUrl.toString())
                .get()
                .response()
                .as(StepVerifier::create)
                .assertNext(res -> assertThat(res.status()).isEqualTo(HttpResponseStatus.OK))
                .verifyComplete();
    }

    @RepeatedTest(10)
    void webClient() {
        WebClient client = WebClient.create(baseUrl.toString());

        server.enqueue(new MockResponse());
        client.get().retrieve().toBodilessEntity().then().as(StepVerifier::create).verifyComplete();
    }
}

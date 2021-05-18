package com.github.hisener.spring.webflux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.github.hisener.spring.webflux.MicrometerTest.TestApplication;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Mono;

@SpringBootTest(
        classes = TestApplication.class,
        properties = "management.endpoints.web.exposure.include=prometheus",
        webEnvironment = RANDOM_PORT)
final class MicrometerTest {
    @Autowired private WebTestClient webTestClient;

    @Test
    void prometheus() {
        assertThat(
                        webTestClient
                                .get()
                                .uri("/actuator/prometheus")
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBody()
                                .returnResult()
                                .toString())
                .contains("test_blocking_gauge 1.0");
    }

    @EnableWebFlux
    @EnableAutoConfiguration
    @Import(BlockingMeterBinder.class)
    static class TestApplication {}

    private static final class BlockingMeterBinder implements MeterBinder {
        @Override
        public void bindTo(MeterRegistry registry) {
            Gauge.builder(
                            "test.blocking.gauge",
                            Mono.fromCallable(
                                            () -> {
                                                // This blocking operation is fine because it's
                                                // scheduled on the elastic scheduler. See
                                                // `AbstractWebFluxEndpointHandlerMapping.ElasticSchedulerInvoker.invoke`.
                                                Thread.sleep(10);
                                                return 1;
                                            })
                                    ::block)
                    .register(registry);
        }
    }
}

package com.github.hisener.spring.webclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

/**
 * Minimal reproducible example for {@code AnnotatedConnectException}s in Spring WebClient due to
 * multiple entries in {@code /etc/hosts} file. It turned out it's netty issue.
 *
 * <p>See:
 *
 * <ul>
 *   <li>https://github.com/reactor/reactor-netty/issues/1405
 *   <li>https://github.com/netty/netty/issues/10834
 */
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

    @BeforeEach
    void setUp() {
        server.enqueue(new MockResponse());
    }

    @AfterAll
    void tearDown() throws IOException {
        StepVerifier.resetDefaultTimeout();
        server.close();
    }

    @Test
    void apacheClient() throws IOException {
        assertThat(apacheClient.execute(new HttpGet(baseUrl)).getStatusLine().getStatusCode())
                .isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    void nettyClient() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();

            bootstrap
                    .group(group)
                    .channel(NioSocketChannel.class)
                    // There is no issue with DefaultAddressResolverGroup
                    // .resolver(DefaultAddressResolverGroup.INSTANCE)
                    .resolver(
                            new DnsAddressResolverGroup(
                                    new DnsNameResolverBuilder(group.next())
                                            .channelType(NioDatagramChannel.class)))
                    .handler(
                            new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel socketChannel) {
                                    ChannelPipeline pipeline = socketChannel.pipeline();
                                    pipeline.addLast(new HttpClientCodec());
                                    pipeline.addLast(
                                            new SimpleChannelInboundHandler<HttpObject>() {
                                                @Override
                                                protected void channelRead0(
                                                        ChannelHandlerContext ctx, HttpObject msg) {
                                                    if (msg instanceof LastHttpContent) {
                                                        ctx.close();
                                                    }
                                                }
                                            });
                                }
                            });

            Channel channel =
                    bootstrap.connect(baseUrl.getHost(), baseUrl.getPort()).sync().channel();
            channel.writeAndFlush(
                    new DefaultFullHttpRequest(
                            HttpVersion.HTTP_1_1, HttpMethod.GET, baseUrl.getRawPath()));
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    @Test
    void reactorNetty() {
        reactor.netty.http.client.HttpClient.create()
                .baseUrl(baseUrl.toString())
                .get()
                .response()
                .as(StepVerifier::create)
                .assertNext(res -> assertThat(res.status()).isEqualTo(HttpResponseStatus.OK))
                .verifyComplete();
    }

    @Test
    void webClient() {
        WebClient client = WebClient.create(baseUrl.toString());

        client.get().retrieve().toBodilessEntity().then().as(StepVerifier::create).verifyComplete();
    }
}

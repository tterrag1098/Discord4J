/*
 * This file is part of Discord4J.
 *
 * Discord4J is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Discord4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J. If not, see <http://www.gnu.org/licenses/>.
 */
package discord4j.gateway;

import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.Opcode;
import discord4j.gateway.json.PayloadData;
import discord4j.gateway.payload.PayloadReader;
import discord4j.gateway.payload.PayloadWriter;
import discord4j.websocket.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.*;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.util.logging.Level;

/**
 * Represents a websocket handler specialized for Discord gateway operations.
 * <p>
 * It includes a zlib-based decompressor and dedicated handling of closing events that normally occur during Discord
 * gateway lifecycle, and provides methods for inbound and outbound payload operations.
 * <p>
 * The handler also provides two methods to control the lifecycle and proper cleanup, like {@link #close()} and
 * {@link #error(Throwable)} which perform operations on the current gateway connection. It is required to use these
 * methods to signal closure and errors in order to cleanly complete the session.
 * <p>
 * All payloads going through this handler are passed to the given {@link discord4j.gateway.payload.PayloadReader}
 * and {@link discord4j.gateway.payload.PayloadWriter}.
 * <h2>Example usage</h2>
 * <pre>
 * // pull operation - inbound
 * handler.inbound().subscribe(payload -&gt; {
 *     if (payload.getData() instanceof Hello) {
 *         IdentifyProperties properties = new IdentifyProperties(...);
 *         GatewayPayload&lt;Identify&gt; identify = GatewayPayload.identify(...);
 *
 *         handler.outbound().next(identify); // push operation - outbound
 *     }
 * }, error -&gt; {
 *     log.warn("Gateway connection terminated: {}", error.toString());
 * });
 * </pre>
 */
public class DiscordWebSocketHandler implements WebSocketHandler {

    private static final Logger log = Loggers.getLogger(DiscordWebSocketHandler.class);

    private static final Logger closeLogger = Loggers.getLogger("discord4j.gateway.session.close");
    private static final Logger inboundLogger = Loggers.getLogger("discord4j.gateway.session.inbound");
    private static final Logger outboundLogger = Loggers.getLogger("discord4j.gateway.session.outbound");

    private final ZlibDecompressor decompressor = new ZlibDecompressor();
    private final UnicastProcessor<GatewayPayload<?>> inboundExchange = UnicastProcessor.create();
    private final UnicastProcessor<GatewayPayload<?>> outboundExchange = UnicastProcessor.create();
    private final MonoProcessor<Void> completionNotifier = MonoProcessor.create();
    private final TokenBucket limiter = new TokenBucket(120, Duration.ofSeconds(60));

    private final PayloadReader reader;
    private final PayloadWriter writer;
    private final FluxSink<GatewayPayload<?>> inboundSink;
    private final FluxSink<GatewayPayload<?>> outboundSink;

    /**
     * Create a new handler with the given payload reader and writer.
     *
     * @param reader the PayloadReader to process each inbound payload
     * @param writer the PayloadWriter to process each outbound payload
     */
    public DiscordWebSocketHandler(PayloadReader reader, PayloadWriter writer) {
        this.reader = reader;
        this.writer = writer;
        this.inboundSink = inboundExchange.sink(FluxSink.OverflowStrategy.ERROR);
        this.outboundSink = outboundExchange.sink(FluxSink.OverflowStrategy.LATEST);
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        session.replaceLoggingHandler();

        // Listen to a custom handler's response to retrieve the actual close code and reason, or an error signal if
        // the channel was closed abruptly.
        session.closeFuture()
                .log(closeLogger, Level.FINE, false)
                .map(CloseException::new)
                .subscribe(this::error, this::error);

        session.receive()
                .map(WebSocketMessage::getPayload)
                .compose(decompressor::completeMessages)
                .map(reader::read)
                .log(inboundLogger, Level.FINE, false)
                .subscribe(inboundSink::next, this::error, inboundSink::complete);

        return session.send(outboundExchange.concatMap(this::limitRate)
                .log(outboundLogger, Level.FINE, false)
                .flatMap(this::mapOutbound))
                .then(completionNotifier);
    }

    private Publisher<? extends GatewayPayload<? extends PayloadData>> limitRate(GatewayPayload<?> payload) {
        boolean success = limiter.tryConsume(1);
        if (success) {
            return Mono.just(payload);
        } else {
            return Mono.delay(Duration.ofMillis(limiter.delayMillisToConsume(1)))
                    .map(x -> limiter.tryConsume(1))
                    .map(consumed -> payload);
        }
    }

    private Flux<WebSocketMessage> mapOutbound(GatewayPayload<?> payload) {
        if (payload.getOp() == null) {
            // Gracefully close our gateway
            return Flux.just(WebSocketMessage.close());
        } else if (Opcode.RECONNECT.equals(payload.getOp())) {
            error(new RuntimeException("Reconnecting due to user action"));
            return Flux.empty();
        } else {
            return Flux.just(writer.write(payload)).map(WebSocketMessage::fromText);
        }
    }

    /**
     * Initiates a close sequence that will terminate this session. It will notify all exchanges and the session
     * completion {@link reactor.core.publisher.Mono} in {@link #handle(discord4j.websocket.WebSocketSession)}
     * through a complete signal, dropping all future signals.
     */
    public void close() {
        log.debug("Triggering close sequence");
        completionNotifier.onComplete();
        outboundSink.complete();
    }

    /**
     * Initiates a close sequence with the given error. It will terminate this session with an error signal on the
     * {@link #handle(discord4j.websocket.WebSocketSession)} method, while completing both exchanges through
     * normal complete signals.
     * <p>
     * The error can then be channeled downstream and acted upon accordingly.
     *
     * @param error the cause for this session termination
     */
    public void error(Throwable error) {
        log.debug("Triggering error sequence ({})", error.toString());
        if (!completionNotifier.isTerminated()) {
            completionNotifier.onError(new CloseException(new CloseStatus(1006, error.toString()), error));
        }
        outboundSink.complete();
    }

    /**
     * Obtains the Flux dedicated to all inbound (from the wire) payloads.
     *
     * @return the stream of inbound payloads
     */
    public Flux<GatewayPayload<?>> inbound() {
        return inboundExchange;
    }

    /**
     * Obtains a FluxSink dedicated to all outbound (to the wire) payloads.
     *
     * @return the sink to push outbound payloads
     */
    public FluxSink<GatewayPayload<?>> outbound() {
        return outboundSink;
    }
}

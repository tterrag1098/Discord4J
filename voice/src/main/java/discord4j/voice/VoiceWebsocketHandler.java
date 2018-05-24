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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J.  If not, see <http://www.gnu.org/licenses/>.
 */
package discord4j.voice;

import discord4j.voice.json.VoiceGatewayPayload;
import discord4j.websocket.WebSocketHandler;
import discord4j.websocket.WebSocketMessage;
import discord4j.websocket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.logging.Level;

public class VoiceWebsocketHandler implements WebSocketHandler {

    private static final Logger inboundLogger = Loggers.getLogger("discord4j.voice.inbound");
    private static final Logger outboundLogger = Loggers.getLogger("discord4j.voice.outbound");

    private final UnicastProcessor<VoiceGatewayPayload<?>> inboundExchange = UnicastProcessor.create();
    private final UnicastProcessor<VoiceGatewayPayload<?>> outboundExchange = UnicastProcessor.create();

    private final VoicePayloadReader reader;
    private final VoicePayloadWriter writer;

    public VoiceWebsocketHandler(VoicePayloadReader reader, VoicePayloadWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        session.replaceLoggingHandler();

        session.receive()
                .map(WebSocketMessage::getPayload)
                .map(reader::read)
                .log(inboundLogger, Level.FINE, false)
                .subscribe(inboundExchange::onNext, this::error);

        return session.send(outboundExchange.map(this::mapOutbound).log(outboundLogger, Level.FINE, false));
    }

    public Flux<VoiceGatewayPayload<?>> inbound() {
        return inboundExchange;
    }

    public UnicastProcessor<VoiceGatewayPayload<?>> outbound() {
        return outboundExchange;
    }

    private WebSocketMessage mapOutbound(VoiceGatewayPayload<?> payload) {
        return WebSocketMessage.fromText(writer.write(payload));
    }

    public void close() {
        outboundExchange.onComplete();
        inboundExchange.onComplete();
    }

    public void error(Throwable error) {
        outboundExchange.onComplete();
        inboundExchange.onComplete();
    }
}

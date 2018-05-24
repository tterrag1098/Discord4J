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

import discord4j.common.ResettableInterval;
import discord4j.voice.json.Identify;
import discord4j.voice.json.VoiceGatewayPayload;
import discord4j.websocket.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

public class VoiceGatewayClient {

    public static final String VERSION = "3";

    private final WebSocketClient webSocketClient = new WebSocketClient();
    private final ResettableInterval heartbeat = new ResettableInterval();

    private final EmitterProcessor<VoiceGatewayPayload<?>> sender = EmitterProcessor.create(false);
    private final FluxSink<VoiceGatewayPayload<?>> senderSink = sender.sink(FluxSink.OverflowStrategy.LATEST);

    private final VoiceClient voiceClient;
    private final VoicePayloadReader payloadReader;
    private final VoicePayloadWriter payloadWriter;
    private final VoiceGatewayPayload<Identify> identifyPayload;

    private final AtomicInteger ssrc = new AtomicInteger();

    public VoiceGatewayClient(VoiceClient voiceClient, VoicePayloadReader payloadReader,
                              VoicePayloadWriter payloadWriter, long guildId, long userId, String token,
                              String sessionId) {
        this.voiceClient = voiceClient;
        this.payloadReader = payloadReader;
        this.payloadWriter = payloadWriter;
        this.identifyPayload = VoiceGatewayPayload.identify(Long.toUnsignedString(guildId),
                Long.toUnsignedString(userId), sessionId, token);
    }

    public Mono<Void> execute(String gatewayUrl) {
        return Mono.defer(() -> {
            VoiceWebsocketHandler handler = new VoiceWebsocketHandler(payloadReader, payloadWriter);

            Disposable inboundSub = handler.inbound().subscribe(payload ->
                    VoicePayloadHandlers.handle(payload, voiceClient, this));

            Disposable heartbeatSub = heartbeat.ticks()
                    .map(VoiceGatewayPayload::heartbeat)
                    .subscribe(handler.outbound()::onNext);

            Disposable senderSub = sender.subscribe(handler.outbound()::onNext, t -> handler.close(), handler::close);

            return webSocketClient.execute(gatewayUrl, handler)
                    .doOnTerminate(() -> {
                        inboundSub.dispose();
                        senderSub.dispose();
                        heartbeatSub.dispose();
                        heartbeat.stop();
                    });
        });
    }

    public FluxSink<VoiceGatewayPayload<?>> sender() {
        return senderSink;
    }

    ResettableInterval heartbeat() {
        return heartbeat;
    }

    public AtomicInteger getSsrc() {
        return ssrc;
    }

    VoiceGatewayPayload<Identify> getIdentifyPayload() {
        return identifyPayload;
    }
}

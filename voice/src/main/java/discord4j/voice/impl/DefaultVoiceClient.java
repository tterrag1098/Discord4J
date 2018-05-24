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
package discord4j.voice.impl;

import com.iwebpp.crypto.TweetNaclFast;
import discord4j.voice.*;
import discord4j.voice.json.VoiceGatewayPayload;
import io.netty.buffer.ByteBuf;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.MonoSink;
import reactor.util.concurrent.WaitStrategy;
import reactor.util.function.Tuple2;

public class DefaultVoiceClient implements VoiceClient {

    private final AudioProvider audioProvider;
    private final String endpoint;
    private final VoiceGatewayClient gatewayClient;
    private final VoiceSocket voiceSocket;

    private final MonoProcessor onShutdown = MonoProcessor.create(WaitStrategy.parking());

    public DefaultVoiceClient(VoicePayloadReader payloadReader, VoicePayloadWriter payloadWriter,
                              AudioProvider audioProvider, String endpoint, long guildId, long userId, String token,
                              String sessionId) {
        this.audioProvider = audioProvider;

        this.endpoint = endpoint.replace(":80", ""); // Discord sometimes sends the address with the wrong port.
        this.gatewayClient =
                new VoiceGatewayClient(this, payloadReader, payloadWriter, guildId, userId, token, sessionId);
        this.voiceSocket = new VoiceSocket();
    }

    @Override
    public Mono<Void> execute() {
        return gatewayClient.execute("wss://" + endpoint + "?v=" + VoiceGatewayClient.VERSION);
    }

    @Override
    public Mono<Void> setupUdp(String address, int port) {
        return voiceSocket.setup(address, port);
    }

    @Override
    public Mono<Tuple2<String, Integer>> discoverIp(int ssrc) {
        return voiceSocket.performIpDiscovery(ssrc).cache();
    }

    @Override
    public void sendGatewayMessage(VoiceGatewayPayload<?> message) {
        gatewayClient.sender().next(message);
    }

    @Override
    public void sendAudio(ByteBuf audio) {
        voiceSocket.outbound().next(audio);
    }

    @Override
    public void startSendingAudio(byte[] secretKey, int ssrc) {
        final TweetNaclFast.SecretBox boxer = new TweetNaclFast.SecretBox(secretKey);
        final PacketTransformer transformer = new PacketTransformer(ssrc, boxer);

        Disposable sender = audioProvider.flux()
                .transform(transformer::send)
                .subscribe(this::sendAudio);

        onShutdown.doOnTerminate(sender::dispose).subscribe();
    }

    @Override
    public void shutdown() {
        onShutdown.onComplete();
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }
}

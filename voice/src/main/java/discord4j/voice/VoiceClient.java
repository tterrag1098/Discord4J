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
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.concurrent.atomic.AtomicReference;

public interface VoiceClient {

    Mono<Void> execute();

    Mono<Void> setupUdp(String address, int port);

    Mono<Tuple2<String, Integer>> discoverIp(int ssrc);

    void sendGatewayMessage(VoiceGatewayPayload<?> message);

    void sendAudio(ByteBuf audio);

    void startSendingAudio(byte[] secretKey, int ssrc);

    void shutdown();

    String getEndpoint();
}

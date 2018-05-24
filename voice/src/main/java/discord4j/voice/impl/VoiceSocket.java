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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.ipc.netty.ByteBufFlux;
import reactor.ipc.netty.NettyPipeline;
import reactor.ipc.netty.udp.UdpClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class VoiceSocket {

    public static final String ENCRYPTION_MODE = "xsalsa20_poly1305";
    private static final int DISCOVERY_PACKET_LENGTH = 70;

    private final EmitterProcessor<ByteBuf> inbound = EmitterProcessor.create(false);
    private final EmitterProcessor<ByteBuf> outbound = EmitterProcessor.create(false);

    private final FluxSink<ByteBuf> outboundSink = outbound.sink(FluxSink.OverflowStrategy.LATEST);

    public Mono<Void> setup(String address, int port) {
        return UdpClient.create(address, port)
                .newHandler((in, out) -> {
                    Disposable inboundSub = in.receive().subscribe(inbound::onNext);

                    return out.send(outbound.doOnTerminate(inboundSub::dispose))
                            .options(NettyPipeline.SendOptions::flushOnEach);
                })
                .then();
    }

    public Mono<Tuple2<String, Integer>> performIpDiscovery(int ssrc) {
        Mono<Void> sendDiscoveryPacket = Mono.fromRunnable(() -> {
            ByteBuffer buf = ByteBuffer.allocate(DISCOVERY_PACKET_LENGTH).putInt(ssrc);
            buf.position(0);
            ByteBuf discoveryPacket = Unpooled.wrappedBuffer(buf);

            outbound.onNext(discoveryPacket);
        });

        Mono<Tuple2<String, Integer>> parseResponse = inbound.next()
                .map(buf -> {
                    String address = buf.toString(4, DISCOVERY_PACKET_LENGTH - Short.BYTES, StandardCharsets.UTF_8);
                    int port = buf.readUnsignedShortLE();

                    return Tuples.of(address, port);
                });

        return sendDiscoveryPacket.then(parseResponse);
    }

    public ByteBufFlux inbound() {
        return ByteBufFlux.fromInbound(inbound);
    }

    public FluxSink<ByteBuf> outbound() {
        return outboundSink;
    }
}

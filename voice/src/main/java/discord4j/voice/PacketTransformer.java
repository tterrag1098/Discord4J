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

import com.iwebpp.crypto.TweetNaclFast;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;

public final class PacketTransformer {

    private final Flux<byte[]> headers = Flux.<Character, Character>generate(() -> (char) 0, (c, sink) -> {
        sink.next(c);
        return (char) (c + 1);
    }).map(this::rtpHeader);

    private final int ssrc;
    private final TweetNaclFast.SecretBox boxer;

    public PacketTransformer(int ssrc, TweetNaclFast.SecretBox boxer) {
        this.ssrc = ssrc;
        this.boxer = boxer;
    }

    static FileOutputStream out;

    static {
        try {
            out = new FileOutputStream("out.opus");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Flux<ByteBuf> send(Flux<byte[]> in) {
        return Flux.zip(headers, in)
                .map(t -> {
                    byte[] rtpHeader = t.getT1();
                    byte[] audio = t.getT2();

                    try {
                        out.write(audio);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    byte[] nonce = new byte[24];
                    System.arraycopy(rtpHeader, 0, nonce, 0, 12);

                    return newAudioPacket(rtpHeader, boxer.box(audio, nonce));
                })
                .map(Unpooled::wrappedBuffer)
                .delayElements(Duration.ofMillis(20));
    }

    private byte[] rtpHeader(char seq) {
        return ByteBuffer.allocate(1 + 1 + 2 + 4 + 4)
                .put((byte) 0x80)
                .put((byte) 0x78)
                .putChar(seq)
                .putInt(seq * 960)
                .putInt(ssrc)
                .array();
    }

    private static byte[] newAudioPacket(byte[] rtpHeader, byte[] encryptedAudio) {
        return ByteBuffer.allocate(rtpHeader.length + encryptedAudio.length)
                .put(rtpHeader)
                .put(encryptedAudio)
                .array();
    }

}

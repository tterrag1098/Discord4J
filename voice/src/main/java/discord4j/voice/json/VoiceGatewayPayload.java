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
package discord4j.voice.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import discord4j.voice.json.jackson.OpcodeConverter;
import discord4j.voice.json.jackson.VoicePayloadDeserializer;

import javax.annotation.Nullable;

@JsonDeserialize(using = VoicePayloadDeserializer.class)
public class VoiceGatewayPayload<T extends VoicePayloadData> {

    @JsonSerialize(converter = OpcodeConverter.class)
    private VoiceOpcode<T> op;
    @JsonProperty("d")
    @Nullable
    private T data;

    public VoiceGatewayPayload(VoiceOpcode<T> op, @Nullable T data) {
        this.op = op;
        this.data = data;
    }

    public VoiceGatewayPayload() {
    }

    public static VoiceGatewayPayload<Identify> identify(String serverId, String userId, String sessionId,
                                                         String token) {
        return new VoiceGatewayPayload<>(VoiceOpcode.IDENTIFY, new Identify(serverId, userId, sessionId, token));
    }

    public static VoiceGatewayPayload<SelectProtocol> selectProtocol(String protocol, String address, int port,
                                                                     String mode) {
        return new VoiceGatewayPayload<>(VoiceOpcode.SELECT_PROTOCOL, new SelectProtocol(protocol,
                new SelectProtocol.Data(address, port, mode)));
    }

    public static VoiceGatewayPayload<Heartbeat> heartbeat(long seq) {
        return new VoiceGatewayPayload<>(VoiceOpcode.HEARTBEAT, new Heartbeat(seq));
    }

    public static VoiceGatewayPayload<Speaking> speaking(boolean speaking, int delay, int ssrc) {
        return new VoiceGatewayPayload<>(VoiceOpcode.SPEAKING, new Speaking(speaking, delay, ssrc));
    }

    public static VoiceGatewayPayload<Resume> resume(String serverId, String sessionId, String token) {
        return new VoiceGatewayPayload<>(VoiceOpcode.RESUME, new Resume(serverId, sessionId, token));
    }

    public VoiceOpcode<T> getOp() {
        return op;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "VoiceGatewayPayload{" +
                "op=" + op +
                ", data=" + data +
                '}';
    }
}

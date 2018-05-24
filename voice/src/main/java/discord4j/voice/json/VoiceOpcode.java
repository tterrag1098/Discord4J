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

import javax.annotation.Nullable;

public class VoiceOpcode<T extends VoicePayloadData> {

    public static final VoiceOpcode<Identify> IDENTIFY = newOp(0, Identify.class);
    public static final VoiceOpcode<SelectProtocol> SELECT_PROTOCOL = newOp(1, SelectProtocol.class);
    public static final VoiceOpcode<Ready> READY = newOp(2, Ready.class);
    public static final VoiceOpcode<Heartbeat> HEARTBEAT = newOp(3, Heartbeat.class);
    public static final VoiceOpcode<SessionDescription> SESSION_DESCRIPTION = newOp(4, SessionDescription.class);
    public static final VoiceOpcode<Speaking> SPEAKING = newOp(5, Speaking.class);
    public static final VoiceOpcode<?> HEARTBEAT_ACK = newOp(6, null);
    public static final VoiceOpcode<Resume> RESUME = newOp(7, Resume.class);
    public static final VoiceOpcode<Hello> HELLO = newOp(8, Hello.class);
    public static final VoiceOpcode<?> RESUMED = newOp(9, null);
    public static final VoiceOpcode<ClientDisconnect> CLIENT_DISCONNECT = newOp(13, ClientDisconnect.class);

    private final int rawOp;
    private final Class<T> payloadType;

    private VoiceOpcode(int rawOp, @Nullable Class<T> payloadType) {
        this.rawOp = rawOp;
        this.payloadType = payloadType;
    }

    @Nullable
    public static VoiceOpcode<?> forRaw(int rawOp) {
        switch (rawOp) {
            case 0: return IDENTIFY;
            case 1: return SELECT_PROTOCOL;
            case 2: return READY;
            case 3: return HEARTBEAT;
            case 4: return SESSION_DESCRIPTION;
            case 5: return SPEAKING;
            case 6: return HEARTBEAT_ACK;
            case 7: return RESUME;
            case 8: return HELLO;
            case 9: return RESUMED;
            case 13: return CLIENT_DISCONNECT;
            default: return null;
        }
    }

    private static <T extends VoicePayloadData> VoiceOpcode<T> newOp(int rawOp, @Nullable Class<T> payloadType) {
        return new VoiceOpcode<>(rawOp, payloadType);
    }

    public int getRawOp() {
        return rawOp;
    }

    @Nullable
    public Class<T> getPayloadType() {
        return payloadType;
    }

    @Override
    public String toString() {
        return Integer.toString(getRawOp());
    }
}

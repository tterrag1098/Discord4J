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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

@JsonIgnoreProperties({"video_codec", "media_session_id", "audio_codec"})
public class SessionDescription implements VoicePayloadData {

    private String mode;
    @JsonProperty("secret_key")
    private byte[] secretKey;

    public String getMode() {
        return mode;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    @Override
    public String toString() {
        return "SessionDescription{" +
                "mode='" + mode + '\'' +
                ", secretKey=" + Arrays.toString(secretKey) +
                '}';
    }
}

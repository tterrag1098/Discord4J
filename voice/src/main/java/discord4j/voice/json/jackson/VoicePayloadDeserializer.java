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
package discord4j.voice.json.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import discord4j.voice.json.VoiceOpcode;
import discord4j.voice.json.VoicePayloadData;
import discord4j.voice.json.VoiceGatewayPayload;

import java.io.IOException;

public class VoicePayloadDeserializer extends StdDeserializer<VoiceGatewayPayload<?>> {

    public VoicePayloadDeserializer() {
        super(VoiceGatewayPayload.class);
    }

    @Override
    public VoiceGatewayPayload<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode payload = p.getCodec().readTree(p);

//        System.out.println(payload);

        VoiceOpcode<?> op = VoiceOpcode.forRaw(payload.get("op").asInt());
        Class<? extends VoicePayloadData> payloadType = op == null ? null : op.getPayloadType();
        VoicePayloadData data = payloadType == null ? null : p.getCodec().treeToValue(payload.get("d"), payloadType);

        return new VoiceGatewayPayload(op, data);
    }
}

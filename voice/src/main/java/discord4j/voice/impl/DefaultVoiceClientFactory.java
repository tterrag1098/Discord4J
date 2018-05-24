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

import discord4j.voice.*;

import java.util.concurrent.ConcurrentHashMap;

public class DefaultVoiceClientFactory implements VoiceClientFactory {

    private final ConcurrentHashMap<Long, VoiceClient> voiceClients = new ConcurrentHashMap<>();
    private final VoicePayloadReader payloadReader;
    private final VoicePayloadWriter payloadWriter;

    public DefaultVoiceClientFactory(VoicePayloadReader payloadReader, VoicePayloadWriter payloadWriter) {
        this.payloadReader = payloadReader;
        this.payloadWriter = payloadWriter;
    }

    @Override
    public VoiceClient getVoiceClient(AudioProvider audioProvider, String endpoint, long guildId, long userId, String token, String sessionId) {
        return voiceClients.computeIfAbsent(guildId, k ->
                new DefaultVoiceClient(payloadReader, payloadWriter, audioProvider, endpoint, guildId, userId, token, sessionId));
    }
}

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

import discord4j.voice.impl.VoiceSocket;
import discord4j.voice.json.*;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;

public abstract class VoicePayloadHandlers {

    private static final Logger log = Loggers.getLogger(VoicePayloadHandlers.class);

    @SuppressWarnings("ConstantConditions")
    public static <T extends VoicePayloadData> void handle(VoiceGatewayPayload<T> payload, VoiceClient voiceClient,
                                                           VoiceGatewayClient gateway) {
        switch (payload.getOp().getRawOp()) {
            case 2:  handleReady((Ready) payload.getData(), voiceClient, gateway); break;
            case 4:  handleSessionDescription((SessionDescription) payload.getData(), voiceClient, gateway); break;
            case 5:  handleSpeaking((Speaking) payload.getData(), voiceClient); break;
            case 6:  handleHeartbeatAck(voiceClient); break;
            case 8:  handleHello((Hello) payload.getData(), voiceClient, gateway); break;
            case 9:  handleResumed(voiceClient); break;
            case 13: handleClientDisconnect((ClientDisconnect) payload.getData(), voiceClient); break;
        }
    }

    private static void handleReady(Ready payload, VoiceClient voiceClient, VoiceGatewayClient gateway) {
        log.info("READY");
        System.out.println(payload);

        gateway.getSsrc().set(payload.getSsrc());

        voiceClient.setupUdp(voiceClient.getEndpoint(), payload.getPort())
                .then(voiceClient.discoverIp(payload.getSsrc()))
                .map(t -> VoiceGatewayPayload.selectProtocol("udp", t.getT1(), t.getT2(), VoiceSocket.ENCRYPTION_MODE))
                .subscribe(voiceClient::sendGatewayMessage);
    }

    private static void handleSessionDescription(SessionDescription payload, VoiceClient voiceClient,
                                                 VoiceGatewayClient gateway) {
        log.info("SESSION DESCRIPTION");

        voiceClient.sendGatewayMessage(VoiceGatewayPayload.speaking(true, 0, gateway.getSsrc().get()));
        voiceClient.startHandlingAudio(payload.getSecretKey(), gateway.getSsrc().get());
    }

    private static void handleSpeaking(Speaking payload, VoiceClient voiceClient) {
        log.info("SPEAKING");
    }

    private static void handleHeartbeatAck(VoiceClient voiceClient) {
        log.info("HEARTBEAT ACK");
    }

    private static void handleHello(Hello payload, VoiceClient voiceClient, VoiceGatewayClient gateway) {
        log.info("HELLO");

        Duration interval = Duration.ofMillis(payload.getHeartbeatInterval());
        gateway.heartbeat().start(interval);

        gateway.sender().next(gateway.getIdentifyPayload());
    }

    private static void handleResumed(VoiceClient voiceClient) {
        log.info("RESUMED");
    }

    private static void handleClientDisconnect(ClientDisconnect payload, VoiceClient voiceClient) {
        log.info("CLIENT DISCONNECT");
    }
}

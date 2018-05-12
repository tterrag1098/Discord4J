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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J. If not, see <http://www.gnu.org/licenses/>.
 */

package discord4j.gateway;

import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public interface GatewayClient {

    /**
     * Establish a reconnecting gateway connection to the given URL.
     *
     * @param gatewayUrl the URL used to establish a websocket connection
     * @return a Mono signaling completion
     */
    Mono<Void> execute(String gatewayUrl);

    /**
     * Terminates this client's current gateway connection, and optionally, reconnect to it.
     *
     * @param reconnect if this client should attempt to reconnect after closing
     */
    void close(boolean reconnect);

    /**
     * Obtains the Flux of Dispatch events inbound from the gateway connection made by this client.
     * <p>
     * Can be used like this, for example, to get all created message events:
     * <pre>
     * gatewayClient.dispatch().ofType(MessageCreate.class)
     *     .subscribe(message -&gt; {
     *         System.out.println("Got a message with content: " + message.getMessage().getContent());
     * });
     * </pre>
     *
     * @return a Flux of Dispatch values
     */
    Flux<Dispatch> dispatch();

    /**
     * Obtains the Flux of raw payloads inbound from the gateway connection made by this client.
     *
     * @return a Flux of GatewayPayload values
     */
    Flux<GatewayPayload<?>> receiver();
    /**
     * Retrieves a new FluxSink to safely produce outbound values. By Reactive Streams Specs Rule 2.12 this can't be
     * called twice from the same instance (based on object equality).
     *
     * @return a serializing FluxSink
     */
    FluxSink<GatewayPayload<?>> sender();

    default Flux<?> send(Publisher<GatewayPayload<?>> publisher) {
        return Flux.from(publisher).map(payload -> sender().next(payload));
    }

    /**
     * Retrieve the ID of the current gateway session.
     *
     * @return the ID of the current gateway session. Used for resuming and voice.
     */
    String getSessionId();

    /**
     * Gets the current heartbeat sequence.
     *
     * @return an integer representing the current gateway sequence
     */
    int getLastSequence();
}

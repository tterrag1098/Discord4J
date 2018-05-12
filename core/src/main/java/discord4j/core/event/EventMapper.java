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

package discord4j.core.event;

import discord4j.core.event.dispatch.DispatchContext;
import discord4j.core.event.domain.Event;
import discord4j.gateway.json.dispatch.Dispatch;
import reactor.core.publisher.Mono;

public interface EventMapper {

    /**
     * Process a {@link discord4j.gateway.json.dispatch.Dispatch} object wrapped with its context to
     * potentially obtain an {@link discord4j.core.event.domain.Event}.
     *
     * @param context the DispatchContext used with this Dispatch object
     * @param <D> the Dispatch type
     * @param <E> the resulting Event type
     * @return an Event mapped from the given Dispatch object, or null if no Event is produced.
     */
    <D extends Dispatch, E extends Event> Mono<E> handle(DispatchContext<D> context);
}

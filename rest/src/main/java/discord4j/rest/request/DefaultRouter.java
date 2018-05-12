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
package discord4j.rest.request;

import discord4j.rest.http.client.SimpleHttpClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facilitates the routing of {@link discord4j.rest.request.DiscordRequest DiscordRequests} to the proper
 * {@link discord4j.rest.request.RequestStream RequestStream} according to the bucket in which the request falls.
 */
public class DefaultRouter implements Router {

    private final SimpleHttpClient httpClient;
    private final Scheduler scheduler;
    private final GlobalRateLimiter globalRateLimiter = new GlobalRateLimiter();
    private final Map<BucketKey, RequestStream<?>> streamMap = new ConcurrentHashMap<>();

    public DefaultRouter(SimpleHttpClient httpClient, Scheduler scheduler) {
        this.httpClient = httpClient;
        this.scheduler = scheduler;
    }

    @Override
    public <T> Mono<T> exchange(DiscordRequest<T> request) {
        return Mono.defer(() -> {
            RequestStream<T> stream = getStream(request);
            MonoProcessor<T> callback = MonoProcessor.create();

            stream.push(Tuples.of(callback, request));
            return callback;
        }).publishOn(scheduler);
    }

    @SuppressWarnings("unchecked")
    private <T> RequestStream<T> getStream(DiscordRequest<T> request) {
        return (RequestStream<T>)
                streamMap.computeIfAbsent(BucketKey.of(request.getRoute().getUriTemplate(), request.getCompleteUri()),
                        k -> {
                            RequestStream<T> stream = new RequestStream<>(httpClient, globalRateLimiter);
                            stream.start();
                            return stream;
                        });
    }
}

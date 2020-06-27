/**
 * The MIT License
 *
 * Copyright for portions of unirest-java are held by Kong Inc (c) 2013.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kong.unirest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;


class CacheManager {

    private final CacheWrapper wrapper = new CacheWrapper();
    private final AsyncWrapper asyncWrapper = new AsyncWrapper();
    private final Cache map;

    private Client originalClient;
    private AsyncClient originalAsync;

    public CacheManager() {
        this(100, 0);
    }

    public CacheManager(int depth, long ttl) {
        map = new CacheMap(depth, ttl);
    }

    Client wrap(Client client) {
        this.originalClient = client;
        return wrapper;
    }

    AsyncClient wrapAsync(AsyncClient client) {
        this.originalAsync = client;
        return asyncWrapper;
    }

    private <T> Cache.Key getHash(HttpRequest request, Boolean isAsync, Class<?> responseType) {
        return new Cache.Key(request, isAsync, responseType);
    }

    class CacheWrapper implements Client {

        @Override
        public Object getClient() {
            return originalClient.getClient();
        }

        @Override
        public <T> HttpResponse<T> request(HttpRequest request, Function<RawResponse, HttpResponse<T>> transformer) {
            return request(request, transformer, Object.class);
        }

        @Override
        public <T> HttpResponse<T> request(HttpRequest request,
                                           Function<RawResponse, HttpResponse<T>> transformer,
                                           Class<?> responseType) {

            Cache.Key hash = getHash(request, false, responseType);
            return map.get(hash,
                    () -> originalClient.request(request, transformer, responseType));
        }

        @Override
        public Stream<Exception> close() {
            return originalClient.close();
        }

        @Override
        public void registerShutdownHook() {
            originalClient.registerShutdownHook();
        }
    }

    private class AsyncWrapper implements AsyncClient {
        @Override
        public <T> T getClient() {
            return originalAsync.getClient();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> request(HttpRequest request,
                                                              Function<RawResponse, HttpResponse<T>> transformer,
                                                              CompletableFuture<HttpResponse<T>> callback) {
            return request(request, transformer, callback, Object.class);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> request(HttpRequest request,
                                                              Function<RawResponse, HttpResponse<T>> transformer,
                                                              CompletableFuture<HttpResponse<T>> callback,
                                                              Class<?> responseType) {
            Cache.Key key = getHash(request, true, responseType);
            return map.getAsync(key,
                    () -> originalAsync.request(request, transformer, callback, responseType));
        }

        @Override
        public void registerShutdownHook() {
            originalAsync.registerShutdownHook();
        }

        @Override
        public Stream<Exception> close() {
            return originalAsync.close();
        }

        @Override
        public boolean isRunning() {
            return originalAsync.isRunning();
        }
    }

    private class CacheMap extends LinkedHashMap<Cache.Key, Object> implements Cache {
        private final int maxSize;
        private long ttl;

        CacheMap(int maxSize, long ttl) {
            this.maxSize = maxSize;
            this.ttl = ttl;
        }

        @Override
        public <T> HttpResponse<T> get(Key key, Supplier<HttpResponse<T>> fetcher) {
            clearOld();
            return (HttpResponse<T>)super.computeIfAbsent(key, (k) -> fetcher.get());
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> getAsync(Key key, Supplier<CompletableFuture<HttpResponse<T>>> fetcher) {
            clearOld();
            return (CompletableFuture<HttpResponse<T>>)super.computeIfAbsent(key, (k) -> fetcher.get());
        }

        private void clearOld() {
            if (ttl > 0) {
                Instant now = Util.now();
                keySet().removeIf(k -> ChronoUnit.MILLIS.between(k.getTime(), now) > ttl);
            }
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Object> eldest) {
            return size() > maxSize;
        }

    }

}
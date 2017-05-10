/*
 * This file is part of Vayzd-Spleef, licenced under the MIT Licence (MIT)
 *
 * Copyright (c) Vayzd Network <https://www.vayzd.net/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.vayzd.spleef.http;

import com.google.common.cache.*;
import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.epoll.*;
import io.netty.handler.codec.http.*;
import lombok.*;
import net.vayzd.spleef.*;

import java.net.*;
import java.util.concurrent.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpClient {

    private static final Cache<String, InetAddress> cache = CacheBuilder
            .newBuilder()
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    public static void request(String url, EventLoop loop, final Completable<String> callback) {
        final CountDownLatch latch = new CountDownLatch(1);
        final URI uri = URI.create(url);
        boolean ssl = uri.getScheme().equals("https");
        int port = uri.getPort();
        if (port == -1) {
            switch (uri.getScheme()) {
                case "http":
                    port = 80;
                    break;
                case "https":
                    port = 443;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown scheme " + uri.getScheme());
            }
        }
        InetAddress address = cache.getIfPresent(uri.getHost());
        if (address == null) {
            try {
                address = InetAddress.getByName(uri.getHost());
            } catch (UnknownHostException ex) {
                if (latch.getCount() > 0) {
                    latch.countDown();
                    callback.complete(null);
                }
                ex.printStackTrace();
                return;
            }
            cache.put(uri.getHost(), address);
        }
        final ChannelFutureListener listener = future -> {
            if (future.isSuccess()) {
                String path = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
                request.headers().set("Host", uri.getHost());
                future.channel().writeAndFlush(request);
            } else {
                if (latch.getCount() > 0) {
                    latch.countDown();
                    callback.complete(null);
                }
            }
        };
        new Bootstrap()
                .channel(EpollSocketChannel.class)
                .group(loop)
                .handler(new HttpInitializer(callback, latch, ssl, uri.getHost(), port))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .remoteAddress(address, port)
                .connect()
                .addListener(listener);
    }
}

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

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.*;
import io.netty.handler.timeout.*;
import lombok.*;
import net.vayzd.spleef.*;

import javax.net.ssl.*;
import java.util.concurrent.*;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@SuppressWarnings("deprecation")
final class HttpInitializer extends ChannelInitializer<Channel> {

    private final Completable<String> callback;
    private final CountDownLatch latch;
    private final boolean ssl;
    private final String host;
    private final int port;

    @Override
    protected void initChannel(Channel channel) throws Exception {
        channel.pipeline().addLast("timeout", new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS));
        if (ssl) {
            final SSLEngine engine = SslContext.newClientContext().newEngine(channel.alloc(), host, port);
            channel.pipeline().addLast("ssl", new SslHandler(engine));
        }
        channel.pipeline().addLast("http", new HttpClientCodec());
        channel.pipeline().addLast("handler", new HttpHandler(callback, latch));
    }
}

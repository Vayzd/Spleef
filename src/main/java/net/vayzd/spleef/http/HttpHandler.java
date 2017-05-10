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
import lombok.*;
import net.vayzd.spleef.*;

import java.nio.charset.*;
import java.util.concurrent.*;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)

final class HttpHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final Completable<String> callback;
    private final CountDownLatch latch;
    private final StringBuilder buffer = new StringBuilder();

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable error) throws Exception {
        try {
            if (latch.getCount() > 0) {
                latch.countDown();
                callback.complete(null);
            }
        } finally {
            context.channel().close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, HttpObject object) throws Exception {
        if (object instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) object;
            int code = response.status().code();
            if (code == HttpResponseStatus.NO_CONTENT.code()) {
                done(context);
                return;
            }
            if (code != HttpResponseStatus.OK.code()) {
                throw new IllegalStateException("Expected HTTP response 200 OK, got " + response.status());
            }
        }
        if (object instanceof HttpContent) {
            HttpContent content = (HttpContent) object;
            buffer.append(content.content().toString(Charset.forName("UTF-8")));
            if (object instanceof LastHttpContent) {
                done(context);
            }
        }
    }

    private void done(ChannelHandlerContext context) {
        try {
            callback.complete(buffer.toString());
        } finally {
            context.channel().close();
        }
    }
}

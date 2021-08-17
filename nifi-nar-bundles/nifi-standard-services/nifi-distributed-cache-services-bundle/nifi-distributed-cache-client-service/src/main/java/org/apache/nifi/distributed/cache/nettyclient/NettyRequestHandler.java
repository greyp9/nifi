/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.distributed.cache.nettyclient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.nifi.distributed.cache.nettyclient.adapter.InboundAdapter;

import java.io.IOException;

/**
 * The {@link io.netty.channel.ChannelHandler} responsible for sending client requests and receiving server responses
 * in the context of a distributed cache server.
 */
public class NettyRequestHandler extends ChannelInboundHandlerAdapter {

    /**
     * The object used to buffer and interpret the service response byte stream.
     */
    private InboundAdapter inboundAdapter;

    /**
     * The synchronization construct used to signal the client application that the server response has been received.
     */
    private ChannelPromise channelPromise;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        final ByteBuf byteBuf = (ByteBuf) msg;
        try {
            final byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            inboundAdapter.queue(bytes);
        } finally {
            byteBuf.release();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws IOException {
        inboundAdapter.dequeue();
        if (inboundAdapter.isComplete() && !channelPromise.isSuccess()) {
            channelPromise.setSuccess();
        }
    }

    /**
     * Perform a synchronous method call to the server.  The server is expected to write
     * a byte stream response to the channel, which may be deserialized into a Java object
     * by the caller.
     *
     * @param channel        the network channel used to make the request
     * @param message        the request payload, which might be a method name, and [0..n] concatenated arguments
     * @param inboundAdapter the business logic to deserialize the server response
     */
    public void invoke(final Channel channel, final byte[] message, final InboundAdapter inboundAdapter) {
        final NettyHandshakeHandler handshakeHandler = channel.pipeline().get(NettyHandshakeHandler.class);
        handshakeHandler.waitHandshakeComplete();
        this.inboundAdapter = inboundAdapter;
        channelPromise = channel.newPromise();
        channel.writeAndFlush(Unpooled.wrappedBuffer(message));
        channelPromise.awaitUninterruptibly();
    }
}

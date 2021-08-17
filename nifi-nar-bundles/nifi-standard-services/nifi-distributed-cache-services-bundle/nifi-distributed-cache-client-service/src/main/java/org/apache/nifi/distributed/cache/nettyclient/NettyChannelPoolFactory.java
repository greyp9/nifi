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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.event.transport.netty.channel.pool.InitializingChannelPoolHandler;
import org.apache.nifi.remote.VersionNegotiator;
import org.apache.nifi.ssl.SSLContextService;

import javax.net.ssl.SSLContext;
import java.util.concurrent.TimeUnit;

/**
 * Factory for construction of new {@link ChannelPool}, used by distributed cache clients to invoke service
 * methods.  Cache clients include the NiFi services {@link DistributedSetCacheClientService}
 * and {@link DistributedMapCacheClientService}.
 */
public class NettyChannelPoolFactory {

    /**
     * Instantiate a new netty pool of channels to be used for distributed cache communications
     *
     * @param context           the NiFi configuration to be applied to the channel pool
     * @param versionNegotiator coordinator used to broker the version of the distributed cache protocol with the service
     * @return a channel pool object from which {@link Channel} objects may be obtained
     */
    public static ChannelPool createChannelPool(final ConfigurationContext context, final VersionNegotiator versionNegotiator) {
        final PropertyValue hostname = context.getProperty(DistributedSetCacheClientService.HOSTNAME);
        final PropertyValue port = context.getProperty(DistributedSetCacheClientService.PORT);
        final PropertyValue timeoutMillis = context.getProperty(DistributedSetCacheClientService.COMMUNICATIONS_TIMEOUT);
        final SSLContextService sslContextService = context.getProperty(
                DistributedSetCacheClientService.SSL_CONTEXT_SERVICE).asControllerService(SSLContextService.class);
        final SSLContext sslContext = (sslContextService == null) ? null : sslContextService.createContext();

        final EventLoopGroup group = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap();
        final NettyChannelInitializer initializer = new NettyChannelInitializer(sslContext, versionNegotiator);
        bootstrap.group(group)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis.asTimePeriod(TimeUnit.MILLISECONDS).intValue())
                .remoteAddress(hostname.getValue(), port.asInteger())
                .channel(NioSocketChannel.class);
        final ChannelPoolHandler channelPoolHandler = new InitializingChannelPoolHandler(initializer);
        return new SimpleChannelPool(bootstrap, channelPoolHandler);
    }
}

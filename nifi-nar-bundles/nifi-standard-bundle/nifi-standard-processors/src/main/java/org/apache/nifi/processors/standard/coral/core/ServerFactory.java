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

package org.apache.nifi.processors.standard.coral.core;

import org.apache.nifi.processors.standard.coral.servlet.CoralContentServlet;
import org.apache.nifi.processors.standard.coral.servlet.CoralCreateServlet;
import org.apache.nifi.processors.standard.coral.servlet.CoralFlowfilesServlet;
import org.apache.nifi.processors.standard.coral.servlet.CoralMetadataServlet;
import org.apache.nifi.processors.standard.coral.servlet.CoralServlet;
import org.apache.nifi.security.util.StandardTlsConfiguration;
import org.apache.nifi.security.util.TlsConfiguration;
import org.apache.nifi.util.NiFiBootstrapPropertiesLoader;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.MultipartConfigElement;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ServerFactory {

    public ServerFactory() {
    }

    public Server create(final CoralState coralState, final int port) {
        final NiFiBootstrapPropertiesLoader loader = new NiFiBootstrapPropertiesLoader();
        final Properties properties = new Properties();
        try (final InputStream fis = Files.newInputStream(Paths.get(loader.getDefaultApplicationPropertiesFilePath()))) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final TlsConfiguration tlsConfiguration = StandardTlsConfiguration.fromNiFiProperties(properties);
        final SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(tlsConfiguration.getKeystorePath());
        sslContextFactory.setKeyStorePassword(tlsConfiguration.getKeystorePassword());
        sslContextFactory.setKeyManagerPassword(tlsConfiguration.getKeyPassword());

        // https://www.eclipse.org/jetty/documentation/jetty-9/index.html#jetty-helloworld
        // https://stackoverflow.com/questions/39421686/jetty-pass-object-from-main-method-to-servlet
        final Server server = new Server();

        final ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
        contextHandler.setAttribute(coralState.getClass().getName(), coralState);
        contextHandler.addServlet(CoralServlet.class, "/*");
        contextHandler.addServlet(CoralFlowfilesServlet.class, "/flowfiles/*");
        contextHandler.addServlet(CoralMetadataServlet.class, "/flowfile/metadata/*");
        contextHandler.addServlet(CoralContentServlet.class, "/flowfile/content/*");
        contextHandler.addServlet(CoralCreateServlet.class, "/flowfile/create/*").getRegistration()
                .setMultipartConfig(new MultipartConfigElement(null, 1_048_576L, 1_048_576L, 1_048_576));
        server.setHandler(contextHandler);

        final ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory());
        sslConnector.setPort(port);
        server.addConnector(sslConnector);

        return server;
    }
}

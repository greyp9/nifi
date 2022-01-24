package org.apache.nifi.tests.system.clustering;

import org.apache.nifi.cluster.coordination.node.NodeConnectionState;
import org.apache.nifi.tests.system.InstanceConfiguration;
import org.apache.nifi.tests.system.NiFiClientUtil;
import org.apache.nifi.tests.system.NiFiInstance;
import org.apache.nifi.tests.system.NiFiInstanceFactory;
import org.apache.nifi.tests.system.NiFiSystemIT;
import org.apache.nifi.tests.system.SpawnedClusterNiFiInstanceFactory;
import org.apache.nifi.toolkit.cli.impl.client.nifi.ControllerClient;
import org.apache.nifi.toolkit.cli.impl.client.nifi.NiFiClient;
import org.apache.nifi.toolkit.cli.impl.client.nifi.NiFiClientException;
import org.apache.nifi.web.api.dto.NodeDTO;
import org.apache.nifi.web.api.entity.ClusteSummaryEntity;
import org.apache.nifi.web.api.entity.NodeEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

@RunWith(Parameterized.class)
public class ClusterIT extends NiFiSystemIT {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Parameterized.Parameters
    public static Object[][] data() {
        return new Object[2][0];
    }

    @Override
    protected NiFiInstanceFactory getInstanceFactory() {
        return new SpawnedClusterNiFiInstanceFactory(
                new InstanceConfiguration.Builder()
                        .bootstrapConfig("src/test/resources/conf/clustered/node1/bootstrap.conf")
                        .instanceDirectory("target/node1")
                        .build(),
                new InstanceConfiguration.Builder()
                        .bootstrapConfig("src/test/resources/conf/clustered/node2/bootstrap.conf")
                        .instanceDirectory("target/node2")
                        .build()
        );
    }

    @Override
    protected boolean isDestroyEnvironmentAfterEachTest() {
        return true;
    }

    @Test
    public void testExerciseSystemTestModule() throws IOException {
        logger.trace(getTestName() + "()::ENTER");
        final NiFiInstance niFiInstance = getNiFiInstance();
        final int numberOfNodes = niFiInstance.getNumberOfNodes();
        logger.trace("NiFi instance available, {} nodes.", numberOfNodes);
        for (int i = 1; (i <= numberOfNodes); ++i) {
            final NiFiInstance node = niFiInstance.getNodeInstance(i);
            logger.trace("NiFi node {}", i);
            final Properties properties = node.getProperties();
            properties.stringPropertyNames().stream().filter(k -> k.contains(".port")).sorted()
                    .map(e -> String.format("[%s]=[%s]", e, properties.getProperty(e))).forEach(logger::info);
        }
        logger.trace(getTestName() + "()::EXIT");
    }

    @Test
    public void testDetachClusterNode() throws IOException, NiFiClientException, InterruptedException {
        logger.trace(getTestName() + "()::ENTER");
        final NiFiClient client = getNifiClient();
        final NiFiClientUtil clientUtil = getClientUtil();
        final ClusteSummaryEntity clusterSummary = client.getFlowClient().getClusterSummary();
        final int connectedNodeCount = clusterSummary.getClusterSummary().getConnectedNodeCount();
        Assert.assertEquals(2, connectedNodeCount);

        final ControllerClient controllerClient = client.getControllerClient();
        final String nodeId2 = getNode(controllerClient, 2).getNodeId();

        final NodeEntity nodeDisconnect = clientUtil.disconnectNode(nodeId2);
        Assert.assertTrue(Arrays.asList(NodeConnectionState.DISCONNECTING.name(),
                NodeConnectionState.DISCONNECTED.name()).contains(nodeDisconnect.getNode().getStatus()));

        waitFor(() -> (getNode(controllerClient, nodeId2).getStatus().equals(
                NodeConnectionState.DISCONNECTED.name())));
        Assert.assertEquals(1, client.getFlowClient().getClusterSummary().getClusterSummary()
                .getConnectedNodeCount().intValue());

        final NodeEntity nodeConnect = clientUtil.connectNode(nodeId2);
        Assert.assertTrue(Arrays.asList(NodeConnectionState.CONNECTING.name(),
                NodeConnectionState.CONNECTED.name()).contains(nodeConnect.getNode().getStatus()));

        waitFor(() -> (getNode(controllerClient, nodeId2).getStatus().equals(
                NodeConnectionState.CONNECTED.name())));
        Assert.assertEquals(2, client.getFlowClient().getClusterSummary().getClusterSummary()
                .getConnectedNodeCount().intValue());
        logger.trace(getTestName() + "()::EXIT");
    }

    private NodeDTO getNode(final ControllerClient controllerClient, final int index)
            throws NiFiClientException, IOException {
        final Collection<NodeDTO> nodes = controllerClient.getNodes().getCluster().getNodes();
        return nodes.stream().filter(n -> (n.getApiPort() == CLIENT_API_BASE_PORT + index))
                .findFirst().orElseThrow(() -> new IllegalStateException(getTestName()));
    }

    private NodeDTO getNode(final ControllerClient controllerClient, final String id)
            throws NiFiClientException, IOException {
        final Collection<NodeDTO> nodes = controllerClient.getNodes().getCluster().getNodes();
        return nodes.stream().filter(n -> (n.getNodeId().equals(id)))
                .findFirst().orElseThrow(() -> new IllegalStateException(getTestName()));
    }
}

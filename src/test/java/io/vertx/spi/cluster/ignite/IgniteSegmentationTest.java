/*
 * Copyright (c) 2026 The original author or authors
 * ---------------------------------
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.spi.cluster.ignite;

import io.vertx.Lifecycle;
import io.vertx.LoggingTestWatcher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteState;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.plugin.segmentation.SegmentationPolicy;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.apache.ignite.events.EventType.EVT_NODE_SEGMENTED;
import static org.junit.Assert.assertTrue;

/**
 * A node that Ignite segments has left the cluster for good: {@code leave()} must complete without the cluster.
 */
public class IgniteSegmentationTest {

  private static final String ADDRESSES = "127.0.0.1:47500..47549";

  @Rule
  public LoggingTestWatcher watchman = new LoggingTestWatcher();

  private final List<Vertx> clustered = new ArrayList<>();

  @After
  public void tearDown() throws Exception {
    Lifecycle.close(clustered);
  }

  @Test
  public void testShutdownOnSegmentationCompletes() throws Exception {
    IgniteClusterManager first = start(new IgniteClusterManager(options()));
    start(new IgniteClusterManager(options()));
    IgniteClusterManager segmented = start(new IgniteClusterManager(options()));
    Ignite ignite = segmented.getIgniteInstance();

    segment(first.getIgniteInstance(), ignite);

    // shutdownOnSegmentation closes Vert.x; leave() ends with stopping the Ignite instance it started
    assertTrue("leave() did not complete on the segmented node",
      waitFor(() -> Ignition.state(ignite.name()) == IgniteState.STOPPED, 30_000));
  }

  @Test
  public void testCloseAfterSegmentationOfCustomInstance() throws Exception {
    IgniteClusterManager first = start(new IgniteClusterManager(options()));
    start(new IgniteClusterManager(options()));
    Ignite ignite = Ignition.start(customConfig());
    Vertx vertx = clustered(new IgniteClusterManager(ignite));

    segment(first.getIgniteInstance(), ignite);

    // vertx-ignite only logs the segmentation of a custom instance; the application closes Vert.x
    vertx.close().await(30, TimeUnit.SECONDS);
  }

  private static IgniteOptions options() {
    return new IgniteOptions()
      .setLocalHost("127.0.0.1")
      .setDefaultRegionInitialSize(20L * 1024 * 1024)
      .setDefaultRegionMaxSize(40L * 1024 * 1024)
      .setMetricsLogFrequency(0)
      .setDiscoverySpi(new IgniteDiscoveryOptions()
        .setType("TcpDiscoveryVmIpFinder")
        .setProperties(new JsonObject().put("addresses", new JsonArray().add(ADDRESSES))));
  }

  private static IgniteConfiguration customConfig() {
    return new IgniteConfiguration()
      .setIgniteInstanceName("custom-" + System.nanoTime())
      .setLocalHost("127.0.0.1")
      .setSegmentationPolicy(SegmentationPolicy.NOOP)
      .setBinaryConfiguration(new BinaryConfiguration().setCompactFooter(false))
      .setMetricsLogFrequency(0)
      .setDataStorageConfiguration(new DataStorageConfiguration().setDefaultDataRegionConfiguration(
        new DataRegionConfiguration().setInitialSize(20L * 1024 * 1024).setMaxSize(40L * 1024 * 1024)))
      .setDiscoverySpi(new TcpDiscoverySpi()
        .setIpFinder(new TcpDiscoveryVmIpFinder().setAddresses(Collections.singletonList(ADDRESSES))));
  }

  private IgniteClusterManager start(IgniteClusterManager clusterManager) throws Exception {
    clustered(clusterManager);
    return clusterManager;
  }

  private Vertx clustered(IgniteClusterManager clusterManager) throws Exception {
    Vertx vertx = Vertx.builder().withClusterManager(clusterManager).buildClustered().await(60, TimeUnit.SECONDS);
    clustered.add(vertx);
    return vertx;
  }

  /**
   * Makes the cluster drop {@code victim}: another node fails it, and when the victim receives that it segments
   * itself ({@code EVT_NODE_SEGMENTED}); with {@code SegmentationPolicy.NOOP} Ignite disconnects it from the cluster.
   */
  private static void segment(Ignite other, Ignite victim) throws Exception {
    CountDownLatch segmented = new CountDownLatch(1);
    victim.events().localListen(evt -> {
      segmented.countDown();
      return false;
    }, EVT_NODE_SEGMENTED);
    other.configuration().getDiscoverySpi().failNode(victim.cluster().localNode().id(), null);
    assertTrue("the node was not segmented", segmented.await(30, TimeUnit.SECONDS));
  }

  private static boolean waitFor(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMillis;
    while (!condition.getAsBoolean()) {
      if (System.currentTimeMillis() > deadline) {
        return false;
      }
      Thread.sleep(100);
    }
    return true;
  }
}

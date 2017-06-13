package io.vertx.test.core;

import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;

/**
 * @author Guo Yu
 */
public class IgniteClusterFaultToleranceTest extends FaultToleranceTest {

  @Override
  protected ClusterManager getClusterManager() {
    return new IgniteClusterManager();
  }

}

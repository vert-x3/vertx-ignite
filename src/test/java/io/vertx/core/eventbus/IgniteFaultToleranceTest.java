/*
 * Copyright (c) 2015 The original author or authors
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

package io.vertx.core.eventbus;

import io.vertx.Lifecycle;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;

import java.util.Arrays;
import java.util.List;

/**
 * @author Andrey Gura
 */
public class IgniteFaultToleranceTest extends FaultToleranceTest {

  @Override
  protected ClusterManager getClusterManager() {
    return new IgniteClusterManager();
  }

  @Override
  protected List<String> getExternalNodeSystemProperties() {
    return Arrays.asList(
      "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory",
      "-Djava.net.preferIPv4Stack=true"
    );
  }

  @Override
  protected void afterNodesKilled() throws Exception {
    super.afterNodesKilled();
    // Additional wait to make sure all nodes noticed the shutdowns
    Thread.sleep(10_000);
  }

  @Override
  protected void closeClustered(List<Vertx> clustered) throws Exception {
    Lifecycle.closeClustered(clustered);
  }
}

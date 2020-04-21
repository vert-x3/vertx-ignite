/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.vertx.servicediscovery.impl;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import org.junit.Before;

import static com.jayway.awaitility.Awaitility.await;

/**
 * @author Thomas Segismont
 * @author Lukas Prettenthaler
 */
public class IgniteDiscoveryImplClusteredTest extends DiscoveryImplTestBase {

  @Before
  public void setUp() {
    VertxOptions options = new VertxOptions()
      .setClusterManager(new IgniteClusterManager());
    Vertx.clusteredVertx(options, ar -> {
      vertx = ar.result();
    });
    await().until(() -> vertx != null);
    discovery = new DiscoveryImpl(vertx, new ServiceDiscoveryOptions());
  }
}

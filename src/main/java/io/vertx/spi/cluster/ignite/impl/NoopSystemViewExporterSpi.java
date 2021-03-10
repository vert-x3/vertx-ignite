/*
 * Copyright 2021 Red Hat, Inc.
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
package io.vertx.spi.cluster.ignite.impl;


import org.apache.ignite.spi.IgniteSpiAdapter;
import org.apache.ignite.spi.systemview.ReadOnlySystemViewRegistry;
import org.apache.ignite.spi.systemview.SystemViewExporterSpi;
import org.apache.ignite.spi.systemview.view.SystemView;

import java.util.function.Predicate;

/**
 * No-Op implementation required to disable {@link org.apache.ignite.spi.systemview.jmx.JmxSystemViewExporterSpi}
 *
 * Ignite 2.9 initializes JMX and (if available) SQL system views IFF the array is empty
 *
 * {@linkplain org.apache.ignite.internal.IgnitionEx.IgniteNamedInstance#initializeDefaultSpi(org.apache.ignite.configuration.IgniteConfiguration)}
 * <code>
 *   F.isEmpty(cfg.getSystemViewExporterSpi())
 * </code>
 *
 * @author Markus Spika
 */
public class NoopSystemViewExporterSpi extends IgniteSpiAdapter implements SystemViewExporterSpi {

  @Override
  public void setSystemViewRegistry(ReadOnlySystemViewRegistry registry) {
    // no-op
  }

  @Override
  public void setExportFilter(Predicate<SystemView<?>> filter) {
    // no-op
  }

  @Override
  public void spiStart(String igniteInstanceName) {
    // no-op
  }

  @Override
  public void spiStop() {
    // no-op
  }
}

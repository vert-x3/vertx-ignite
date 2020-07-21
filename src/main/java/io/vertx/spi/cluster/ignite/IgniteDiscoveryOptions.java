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
package io.vertx.spi.cluster.ignite;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.ignite.spi.discovery.DiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.stream.Collectors;

import static org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi.*;
import static org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder.*;

/**
 * @author Lukas Prettenthaler
 */
@DataObject(generateConverter = true)
public class IgniteDiscoveryOptions {
  private String type;
  private JsonObject properties;

  public IgniteDiscoveryOptions() {
    type = "TcpDiscoveryMulticastIpFinder";
    properties = new JsonObject();
  }

  public IgniteDiscoveryOptions(IgniteDiscoveryOptions options) {
    this.type = options.type;
  }

  public IgniteDiscoveryOptions(JsonObject options) {
    this();
    IgniteDiscoveryOptionsConverter.fromJson(options, this);
  }

  public String getType() {
    return type;
  }

  public IgniteDiscoveryOptions setType(String type) {
    this.type = type;
    return this;
  }

  public JsonObject getProperties() {
    return properties;
  }

  public IgniteDiscoveryOptions setProperties(JsonObject properties) {
    this.properties = properties;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    IgniteDiscoveryOptionsConverter.toJson(this, json);
    return json;
  }

  public DiscoverySpi toConfig() {
    switch (type) {
      case "TcpDiscoveryVmIpFinder":
        return new TcpDiscoverySpi()
          .setJoinTimeout(properties.getLong("joinTimeout", DFLT_JOIN_TIMEOUT))
          .setLocalAddress(properties.getString("localAddress", null))
          .setLocalPort(properties.getInteger("localPort", DFLT_PORT))
          .setLocalPortRange(properties.getInteger("localPortRange", DFLT_PORT_RANGE))
          .setIpFinder(new TcpDiscoveryVmIpFinder()
            .setAddresses(properties.getJsonArray("addresses", new JsonArray()).stream()
              .map(Object::toString)
              .collect(Collectors.toList())
            )
          );
      case "TcpDiscoveryMulticastIpFinder":
        return new TcpDiscoverySpi()
          .setJoinTimeout(properties.getLong("joinTimeout", DFLT_JOIN_TIMEOUT))
          .setLocalAddress(properties.getString("localAddress", null))
          .setLocalPort(properties.getInteger("localPort", DFLT_PORT))
          .setLocalPortRange(properties.getInteger("localPortRange", DFLT_PORT_RANGE))
          .setIpFinder(new TcpDiscoveryMulticastIpFinder()
            .setAddressRequestAttempts(properties.getInteger("addressRequestAttempts", DFLT_ADDR_REQ_ATTEMPTS))
            .setLocalAddress(properties.getString("localAddress", null))
            .setMulticastGroup(properties.getString("multicastGroup", DFLT_MCAST_GROUP))
            .setMulticastPort(properties.getInteger("multicastPort", DFLT_MCAST_PORT))
            .setResponseWaitTime(properties.getInteger("responseWaitTime", DFLT_RES_WAIT_TIME))
            .setTimeToLive(properties.getInteger("timeToLive", -1))
            .setAddresses(properties.getJsonArray("addresses", new JsonArray()).stream()
              .map(Object::toString)
              .collect(Collectors.toList())
            )
          );
      default:
        throw new VertxException("not discovery spi found");
    }
  }
}

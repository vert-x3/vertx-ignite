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

package io.vertx.spi.cluster.ignite.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.NodeAddress;
import io.vertx.core.spi.cluster.NodeInfo;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;

/**
 * @author Lukas Prettenthaler
 */
public class IgniteNodeInfo implements Binarylizable {
  private NodeInfo nodeInfo;

  public IgniteNodeInfo() {
  }

  public IgniteNodeInfo(NodeInfo nodeInfo) {
    this.nodeInfo = nodeInfo;
  }

  @Override
  public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
    writer.writeString("host", nodeInfo.getAddress().getHost());
    writer.writeInt("port", nodeInfo.getAddress().getPort());
    JsonObject metadata = nodeInfo.getMetadata();
    writer.writeByteArray("meta", metadata != null ? metadata.toBuffer().getBytes() : null);
  }

  @Override
  public void readBinary(BinaryReader reader) throws BinaryObjectException {
    NodeAddress address = new NodeAddress(reader.readString("host"), reader.readInt("port"));
    byte[] bytes = reader.readByteArray("meta");
    nodeInfo = new NodeInfo(address, bytes != null ? new JsonObject(Buffer.buffer(bytes)) : null);
  }

  public NodeInfo unwrap() {
    return nodeInfo;
  }
}

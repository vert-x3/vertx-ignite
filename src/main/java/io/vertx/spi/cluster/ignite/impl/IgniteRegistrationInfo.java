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

import io.vertx.core.spi.cluster.RegistrationInfo;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;

import java.util.Objects;

/**
 * @author Lukas Prettenthaler
 */
public class IgniteRegistrationInfo implements Binarylizable {
  private String address;
  private RegistrationInfo registrationInfo;

  public IgniteRegistrationInfo() {
  }

  public IgniteRegistrationInfo(String address, RegistrationInfo registrationInfo) {
    this.address = Objects.requireNonNull(address);
    this.registrationInfo = Objects.requireNonNull(registrationInfo);
  }

  public String getAddress() {
    return address;
  }

  public RegistrationInfo getRegistrationInfo() {
    return registrationInfo;
  }

  @Override
  public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
    writer.writeString("address", address);
    writer.writeString("nodeId", registrationInfo.getNodeId());
    writer.writeLong("seq", registrationInfo.getSeq());
    writer.writeBoolean("isLocalOnly", registrationInfo.isLocalOnly());
  }

  @Override
  public void readBinary(BinaryReader reader) throws BinaryObjectException {
    address = reader.readString("address");
    registrationInfo = new RegistrationInfo(reader.readString("nodeId"), reader.readLong("seq"), reader.readBoolean("isLocalOnly"));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IgniteRegistrationInfo that = (IgniteRegistrationInfo) o;

    if (!address.equals(that.address)) return false;
    return registrationInfo.equals(that.registrationInfo);
  }

  @Override
  public int hashCode() {
    int result = address.hashCode();
    result = 31 * result + registrationInfo.hashCode();
    return result;
  }
}

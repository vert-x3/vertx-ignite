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
import io.vertx.core.json.JsonObject;
import org.apache.ignite.cache.*;
import org.apache.ignite.configuration.CacheConfiguration;

import static org.apache.ignite.cache.CacheWriteSynchronizationMode.*;
import static org.apache.ignite.configuration.CacheConfiguration.*;

/**
 * @author Lukas Prettenthaler
 */
@DataObject(generateConverter = true)
public class IgniteCacheOptions {
  private String name;
  private CacheMode cacheMode;
  private int backups;
  private boolean readFromBackup;
  private CacheAtomicityMode atomicityMode;
  private CacheWriteSynchronizationMode writeSynchronizationMode;
  private boolean copyOnRead;
  private long defaultLockTimeout;
  private boolean eagerTtl;
  private boolean encryptionEnabled;
  private String groupName;
  private boolean invalidate;
  private int maxConcurrentAsyncOperations;
  private boolean onheapCacheEnabled;
  private PartitionLossPolicy partitionLossPolicy;
  private CacheRebalanceMode rebalanceMode;
  private int rebalanceOrder;
  private long rebalanceDelay;
  private int maxQueryInteratorsCount;
  private boolean eventsDisabled;

  public IgniteCacheOptions() {
    atomicityMode = DFLT_CACHE_ATOMICITY_MODE;
    writeSynchronizationMode = PRIMARY_SYNC;
    cacheMode = DFLT_CACHE_MODE;
    backups = DFLT_BACKUPS;
    readFromBackup = DFLT_READ_FROM_BACKUP;
    copyOnRead = DFLT_COPY_ON_READ;
    defaultLockTimeout = DFLT_LOCK_TIMEOUT;
    eagerTtl = DFLT_EAGER_TTL;
    invalidate = DFLT_INVALIDATE;
    maxConcurrentAsyncOperations = DFLT_MAX_CONCURRENT_ASYNC_OPS;
    partitionLossPolicy = DFLT_PARTITION_LOSS_POLICY;
    rebalanceMode = DFLT_REBALANCE_MODE;
    maxQueryInteratorsCount = DFLT_MAX_QUERY_ITERATOR_CNT;
    eventsDisabled = DFLT_EVENTS_DISABLED;
  }

  public IgniteCacheOptions(IgniteCacheOptions options) {
    this.name = options.name;
    this.cacheMode = options.cacheMode;
    this.backups = options.backups;
    this.readFromBackup = options.readFromBackup;
    this.atomicityMode = options.atomicityMode;
    this.writeSynchronizationMode = options.writeSynchronizationMode;
    this.copyOnRead = options.copyOnRead;
    this.defaultLockTimeout = options.defaultLockTimeout;
    this.eagerTtl = options.eagerTtl;
    this.encryptionEnabled = options.encryptionEnabled;
    this.groupName = options.groupName;
    this.invalidate = options.invalidate;
    this.maxConcurrentAsyncOperations = options.maxConcurrentAsyncOperations;
    this.onheapCacheEnabled = options.onheapCacheEnabled;
    this.partitionLossPolicy = options.partitionLossPolicy;
    this.rebalanceMode = options.rebalanceMode;
    this.rebalanceOrder = options.rebalanceOrder;
    this.rebalanceDelay = options.rebalanceDelay;
    this.maxQueryInteratorsCount = options.maxQueryInteratorsCount;
    this.eventsDisabled = options.eventsDisabled;
  }

  public IgniteCacheOptions(JsonObject options) {
    this();
    IgniteCacheOptionsConverter.fromJson(options, this);
  }

  public String getName() {
    return name;
  }

  public IgniteCacheOptions setName(String name) {
    this.name = name;
    return this;
  }

  public String getCacheMode() {
    return cacheMode.name();
  }

  public IgniteCacheOptions setCacheMode(String cacheMode) {
    this.cacheMode = CacheMode.valueOf(cacheMode);
    return this;
  }

  public int getBackups() {
    return backups;
  }

  public IgniteCacheOptions setBackups(int backups) {
    this.backups = backups;
    return this;
  }

  public boolean isReadFromBackup() {
    return readFromBackup;
  }

  public IgniteCacheOptions setReadFromBackup(boolean readFromBackup) {
    this.readFromBackup = readFromBackup;
    return this;
  }

  public String getAtomicityMode() {
    return atomicityMode.name();
  }

  public IgniteCacheOptions setAtomicityMode(String atomicityMode) {
    this.atomicityMode = CacheAtomicityMode.valueOf(atomicityMode);
    return this;
  }

  public String getWriteSynchronizationMode() {
    return writeSynchronizationMode.name();
  }

  public IgniteCacheOptions setWriteSynchronizationMode(String writeSynchronizationMode) {
    this.writeSynchronizationMode = valueOf(writeSynchronizationMode);
    return this;
  }

  public boolean isCopyOnRead() {
    return copyOnRead;
  }

  public IgniteCacheOptions setCopyOnRead(boolean copyOnRead) {
    this.copyOnRead = copyOnRead;
    return this;
  }

  public long getDefaultLockTimeout() {
    return defaultLockTimeout;
  }

  public IgniteCacheOptions setDefaultLockTimeout(long defaultLockTimeout) {
    this.defaultLockTimeout = defaultLockTimeout;
    return this;
  }

  public boolean isEagerTtl() {
    return eagerTtl;
  }

  public IgniteCacheOptions setEagerTtl(boolean eagerTtl) {
    this.eagerTtl = eagerTtl;
    return this;
  }

  public boolean isEncryptionEnabled() {
    return encryptionEnabled;
  }

  public IgniteCacheOptions setEncryptionEnabled(boolean encryptionEnabled) {
    this.encryptionEnabled = encryptionEnabled;
    return this;
  }

  public String getGroupName() {
    return groupName;
  }

  public IgniteCacheOptions setGroupName(String groupName) {
    this.groupName = groupName;
    return this;
  }

  public boolean isInvalidate() {
    return invalidate;
  }

  public IgniteCacheOptions setInvalidate(boolean invalidate) {
    this.invalidate = invalidate;
    return this;
  }

  public int getMaxConcurrentAsyncOperations() {
    return maxConcurrentAsyncOperations;
  }

  public IgniteCacheOptions setMaxConcurrentAsyncOperations(int maxConcurrentAsyncOperations) {
    this.maxConcurrentAsyncOperations = maxConcurrentAsyncOperations;
    return this;
  }

  public boolean isOnheapCacheEnabled() {
    return onheapCacheEnabled;
  }

  public IgniteCacheOptions setOnheapCacheEnabled(boolean onheapCacheEnabled) {
    this.onheapCacheEnabled = onheapCacheEnabled;
    return this;
  }

  public String getPartitionLossPolicy() {
    return partitionLossPolicy.name();
  }

  public IgniteCacheOptions setPartitionLossPolicy(String partitionLossPolicy) {
    this.partitionLossPolicy = PartitionLossPolicy.valueOf(partitionLossPolicy);
    return this;
  }

  public String getRebalanceMode() {
    return rebalanceMode.name();
  }

  public IgniteCacheOptions setRebalanceMode(String rebalanceMode) {
    this.rebalanceMode = CacheRebalanceMode.valueOf(rebalanceMode);
    return this;
  }

  public int getRebalanceOrder() {
    return rebalanceOrder;
  }

  public IgniteCacheOptions setRebalanceOrder(int rebalanceOrder) {
    this.rebalanceOrder = rebalanceOrder;
    return this;
  }

  public long getRebalanceDelay() {
    return rebalanceDelay;
  }

  public IgniteCacheOptions setRebalanceDelay(long rebalanceDelay) {
    this.rebalanceDelay = rebalanceDelay;
    return this;
  }

  public int getMaxQueryInteratorsCount() {
    return maxQueryInteratorsCount;
  }

  public IgniteCacheOptions setMaxQueryInteratorsCount(int maxQueryInteratorsCount) {
    this.maxQueryInteratorsCount = maxQueryInteratorsCount;
    return this;
  }

  public boolean isEventsDisabled() {
    return eventsDisabled;
  }

  public IgniteCacheOptions setEventsDisabled(boolean eventsDisabled) {
    this.eventsDisabled = eventsDisabled;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    IgniteCacheOptionsConverter.toJson(this, json);
    return json;
  }

  public CacheConfiguration toConfig() {
    return new CacheConfiguration<>()
      .setName(name)
      .setCacheMode(cacheMode)
      .setBackups(backups)
      .setReadFromBackup(readFromBackup)
      .setAtomicityMode(atomicityMode)
      .setWriteSynchronizationMode(writeSynchronizationMode)
      .setCopyOnRead(copyOnRead)
      .setDefaultLockTimeout(defaultLockTimeout)
      .setEagerTtl(eagerTtl)
      .setEncryptionEnabled(encryptionEnabled)
      .setGroupName(groupName)
      .setInvalidate(invalidate)
      .setMaxConcurrentAsyncOperations(maxConcurrentAsyncOperations)
      .setOnheapCacheEnabled(onheapCacheEnabled)
      .setPartitionLossPolicy(partitionLossPolicy)
      .setRebalanceMode(rebalanceMode)
      .setRebalanceOrder(rebalanceOrder)
      .setRebalanceDelay(rebalanceDelay)
      .setMaxQueryIteratorsCount(maxQueryInteratorsCount)
      .setEventsDisabled(eventsDisabled);
  }
}

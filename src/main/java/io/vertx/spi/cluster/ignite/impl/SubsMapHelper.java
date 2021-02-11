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
package io.vertx.spi.cluster.ignite.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.VertxException;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.spi.cluster.NodeSelector;
import io.vertx.core.spi.cluster.RegistrationInfo;
import io.vertx.core.spi.cluster.RegistrationUpdateEvent;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.ScanQuery;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.event.CacheEntryEvent;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * @author Thomas Segismont
 * @author Lukas Prettenthaler
 */
public class SubsMapHelper {
  private final IgniteCache<IgniteRegistrationInfo, Boolean> map;
  private boolean shutdown;

  public SubsMapHelper(Ignite ignite, NodeSelector nodeSelector, VertxInternal vertxInternal) {
    map = ignite.getOrCreateCache("__vertx.subs");
    map.query(new ContinuousQuery<IgniteRegistrationInfo, Boolean>()
      .setAutoUnsubscribe(true)
      .setTimeInterval(100L)
      .setPageSize(128)
      .setLocalListener(l -> listen(l, nodeSelector, vertxInternal)));
    shutdown = false;
  }

  public void get(String address, Promise<List<RegistrationInfo>> promise) {
    if (shutdown) {
      promise.complete(null);
      return;
    }
    try {
      List<RegistrationInfo> infos = map.query(new ScanQuery<IgniteRegistrationInfo, Boolean>((k, v) -> k.address().equals(address)))
        .getAll().stream()
        .map(Cache.Entry::getKey)
        .map(IgniteRegistrationInfo::registrationInfo)
        .collect(toList());
      promise.complete(infos);
    } catch (IllegalStateException | CacheException e) {
      promise.fail(new VertxException(e));
    }
  }

  public Future<Void> put(String address, RegistrationInfo registrationInfo) {
    if (shutdown) {
      return Future.failedFuture(new VertxException("shutdown in progress"));
    }
    try {
      map.put(new IgniteRegistrationInfo(address, registrationInfo), Boolean.TRUE);
    } catch (IllegalStateException | CacheException e) {
      return Future.failedFuture(new VertxException(e));
    }
    return Future.succeededFuture();
  }

  public void remove(String address, RegistrationInfo registrationInfo, Promise<Void> promise) {
    if (shutdown) {
      promise.complete();
      return;
    }
    try {
      map.remove(new IgniteRegistrationInfo(address, registrationInfo));
      promise.complete();
    } catch (IllegalStateException | CacheException e) {
      promise.fail(new VertxException(e));
    }
  }

  public void removeAllForNode(String nodeId) {
    TreeSet<IgniteRegistrationInfo> toRemove = map.query(new ScanQuery<IgniteRegistrationInfo, Boolean>((k, v) -> k.registrationInfo().nodeId().equals(nodeId)))
      .getAll().stream()
      .map(Cache.Entry::getKey)
      .collect(Collectors.toCollection(TreeSet::new));
    try {
      map.removeAll(toRemove);
    } catch (IllegalStateException | CacheException t) {
      //ignore
    }
  }

  public void leave() {
    shutdown = true;
  }

  private void listen(final Iterable<CacheEntryEvent<? extends IgniteRegistrationInfo, ? extends Boolean>> events, final NodeSelector nodeSelector, final VertxInternal vertxInternal) {
    vertxInternal.<List<RegistrationInfo>>executeBlocking(promise -> {
      StreamSupport.stream(events.spliterator(), false)
        .map(e -> e.getKey().address())
        .distinct()
        .forEach(address -> {
          Promise<List<RegistrationInfo>> prom = Promise.promise();
          prom.future().onSuccess(registrationInfos -> {
            nodeSelector.registrationsUpdated(new RegistrationUpdateEvent(address, registrationInfos));
          });
          get(address, prom);
        });
      promise.complete();
    });
  }
}

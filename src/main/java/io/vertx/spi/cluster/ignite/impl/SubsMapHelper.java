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
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.spi.cluster.RegistrationInfo;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.lang.IgnitePredicate;

import javax.cache.Cache;
import javax.cache.CacheException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_PUT;
import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_REMOVED;

/**
 * @author Thomas Segismont
 * @author Lukas Prettenthaler
 */
public class SubsMapHelper {
  private final IgniteCache<IgniteRegistrationInfo, Boolean> map;
  private final Map<String, Handler<IgniteRegistrationInfo>> entryListeners;

  public SubsMapHelper(Ignite ignite) {
    map = ignite.getOrCreateCache("__vertx.subs");
    entryListeners = new ConcurrentHashMap<>();

    ignite.events().localListen((IgnitePredicate<Event>) event -> {
      if (!(event instanceof CacheEvent)) {
        return true;
      }
      CacheEvent cacheEvent = (CacheEvent) event;
      if (!Objects.equals(cacheEvent.cacheName(), map.getName())) {
        return true;
      }
      entryListeners.values().forEach(h -> h.handle(cacheEvent.key()));
      return true;
    }, EVT_CACHE_OBJECT_PUT, EVT_CACHE_OBJECT_REMOVED);
  }

  public Future<List<RegistrationInfo>> get(String address) {
    try {
      List<RegistrationInfo> infos = map.query(new ScanQuery<IgniteRegistrationInfo, Boolean>((k, v) -> k.getAddress().equals(address)))
        .getAll().stream()
        .map(Cache.Entry::getKey)
        .map(IgniteRegistrationInfo::getRegistrationInfo)
        .collect(toList());
      return Future.succeededFuture(infos);
    } catch(IllegalStateException | CacheException e) {
      return Future.failedFuture(new VertxException(e));
    }
  }

  public Future<Void> put(String address, RegistrationInfo registrationInfo) {
    try {
      map.put(new IgniteRegistrationInfo(address, registrationInfo), Boolean.TRUE);
    } catch (IllegalStateException | CacheException e) {
      return Future.failedFuture(new VertxException(e));
    }
    return Future.succeededFuture();
  }

  public Future<Void> remove(String address, RegistrationInfo registrationInfo) {
    try {
      map.remove(new IgniteRegistrationInfo(address, registrationInfo));
    } catch (IllegalStateException | CacheException e) {
      return Future.failedFuture(new VertxException(e));
    }
    return Future.succeededFuture();
  }

  public void removeAllForNode(String nodeId) {
    List<IgniteRegistrationInfo> toRemove = map.query(new ScanQuery<IgniteRegistrationInfo, Boolean>((k, v) -> k.getRegistrationInfo().getNodeId().equals(nodeId)))
      .getAll().stream()
      .map(Cache.Entry::getKey)
      .collect(Collectors.toList());
    for (IgniteRegistrationInfo info : toRemove) {
      try {
        map.remove(info);
      } catch (IllegalStateException | CacheException t) {
        //ignore
      }
    }
  }

  public String addEntryListener(String address, Runnable callback) {
    String listenerId = UUID.randomUUID().toString();
    entryListeners.put(listenerId, new MapListener(address, callback));
    return listenerId;
  }

  public void removeEntryListener(String listenerId) {
    entryListeners.remove(listenerId);
  }

  private static class MapListener implements Handler<IgniteRegistrationInfo> {
    final String address;
    final Runnable callback;

    MapListener(String address, Runnable callback) {
      this.address = address;
      this.callback = callback;
    }

    @Override
    public void handle(IgniteRegistrationInfo registrationInfo) {
      if (address.equals(registrationInfo.getAddress())) {
        callback.run();
      }
    }
  }
}

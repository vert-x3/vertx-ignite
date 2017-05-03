package io.vertx.spi.cluster.ignite.cfg;

import java.util.Map;

import javax.management.MBeanServer;

import org.apache.ignite.IgniteLogger;
import org.apache.ignite.events.Event;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.lifecycle.LifecycleBean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public interface IgniteConfigurationMixin {

  @JsonIgnore Map<IgnitePredicate<? extends Event>, int[]> getLocalEventListeners();
  @JsonIgnore MBeanServer getMBeanServer();
  @JsonIgnore LifecycleBean[] getLifecycleBeans();
  @JsonIgnore IgniteLogger getGridLogger();
  
  @JsonDeserialize(converter = EventTypeConverter.class)
  int[] getIncludeEventTypes();
  
}

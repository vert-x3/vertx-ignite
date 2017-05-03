package io.vertx.spi.cluster.ignite.cfg;

import java.util.UUID;

import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.util.typedef.internal.A;
import org.apache.ignite.logger.LoggerNodeIdAware;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Logger to use with Vertx logging. Implementation simply delegates to Vertx
 * Logging.
 */
public class VertxLogger implements IgniteLogger, LoggerNodeIdAware {

  private final Logger logger;

  private static final Object mux = new Object();

  /** Node ID. */
  private volatile UUID nodeId;

  public VertxLogger() {
    this.logger = LoggerFactory.getLogger("global");
  }

  public VertxLogger(Logger logger) {
    this.logger = logger;
  }

  /** {@inheritDoc} */
  @Override
  public void setNodeId(UUID nodeId) {
    A.notNull(nodeId, "nodeId");

    if (this.nodeId != null)
      return;

    synchronized (mux) {
      // Double check.
      if (this.nodeId != null)
        return;

      this.nodeId = nodeId;
    }
  }

  @Override
  public UUID getNodeId() {
    return nodeId;
  }

  @Override
  public IgniteLogger getLogger(Object ctgr) {
    return new VertxLogger(ctgr == null ? LoggerFactory.getLogger("")
        : LoggerFactory.getLogger(ctgr instanceof Class ? ((Class) ctgr).getName() : String.valueOf(ctgr)));
  }

  @Override
  public void trace(String msg) {
    if (nodeId != null)
      logger.trace(logNodeId(msg));
  }

  private String logNodeId(String originalMessage) {
    StringBuilder builder = new StringBuilder();
    builder.append("Ignite NODEID: ").append(nodeId).append(". ").append(originalMessage);
    return builder.toString();
  }

  @Override
  public void debug(String msg) {
    if (nodeId != null)
      logger.debug(msg);
  }

  @Override
  public void info(String msg) {
    if (nodeId != null)
      logger.info(msg);
  }

  @Override
  public void warning(String msg) {
    if (nodeId != null)
      logger.warn(msg);
  }

  @Override
  public void warning(String msg, Throwable e) {
    if (nodeId != null)
      logger.warn(msg, e);
  }

  @Override
  public void error(String msg) {
    if (nodeId != null)
      logger.error(msg);
  }

  @Override
  public void error(String msg, Throwable e) {
    if (nodeId != null)
      logger.error(msg, e);
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public boolean isQuiet() {
    return nodeId == null;
  }

  @Override
  public String fileName() {
    return null;
  }

}

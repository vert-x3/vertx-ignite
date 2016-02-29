package examples;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Examples {

  public void example1() {
    ClusterManager clusterManager = new IgniteClusterManager();

    VertxOptions options = new VertxOptions().setClusterManager(clusterManager);
    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
      } else {
        // failed!
      }
    });
  }

  public void example2() {
    IgniteConfiguration cfg = new IgniteConfiguration();
    // Configuration code (omitted)

    ClusterManager clusterManager = new IgniteClusterManager(cfg);

    VertxOptions options = new VertxOptions().setClusterManager(clusterManager);
    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
      } else {
        // failed!
      }
    });
  }

}

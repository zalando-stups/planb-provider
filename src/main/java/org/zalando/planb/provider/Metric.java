package org.zalando.planb.provider;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

public class Metric {

    private static final Logger LOG = getLogger(Metric.class);

    private final MetricRegistry metricRegistry;

    private Long start;

    public Metric(final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void start() {
        this.start = System.currentTimeMillis();
    }

    public void finish(Supplier<String> keySupplier) {
        if (started()) {
            try {
                final long end = System.currentTimeMillis();
                final Timer timer = metricRegistry.timer(keySupplier.get());
                timer.update(end - start, MILLISECONDS);
            } catch (Exception e) {
                LOG.warn("Unable to submit timer metric", e);
            }
        }
    }

    private boolean started() {
        return start != null;
    }

    public static String trimSlash(String realm) {
        return realm.startsWith("/") ? realm.substring(1) : realm;
    }

}

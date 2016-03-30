package org.zalando.planb.provider;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class CassandraHealthIndicator extends AbstractHealthIndicator {

    private final Session session;
    private final Statement healthCheck;

    @Autowired
    public CassandraHealthIndicator(final Session session, final CassandraProperties properties) {
        this.session = checkNotNull(session, "Cassandra session must not be null");
        this.healthCheck = this.session.prepare(properties.getHealthCheckQuery())
                .setConsistencyLevel(properties.getReadConsistencyLevel())
                .bind();
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {

        // if this throws an exception the AbstractHealthIndicator implementation will report this indicator as "down".
        session.execute(healthCheck);
        builder.up();
    }
}

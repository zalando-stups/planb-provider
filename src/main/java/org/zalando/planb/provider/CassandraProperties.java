package org.zalando.planb.provider;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ProtocolOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.datastax.driver.core.ConsistencyLevel.ONE;

@ConfigurationProperties(prefix = "cassandra")
public class CassandraProperties {
    private String keyspace;
    /**
     * Comma-separated string list of hosts
     */
    private String contactPoints = "localhost";
    private String clusterName;
    private int port = ProtocolOptions.DEFAULT_PORT;
    private ConsistencyLevel writeConsistencyLevel = ONE; // TODO what is a sane default?
    private ConsistencyLevel readConsistencyLevel = ONE; // TODO what is a sane default?

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public String getContactPoints() {
        return contactPoints;
    }

    public void setContactPoints(String contactPoints) {
        this.contactPoints = contactPoints;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ConsistencyLevel getWriteConsistencyLevel() {
        return writeConsistencyLevel;
    }

    public void setWriteConsistencyLevel(ConsistencyLevel writeConsistencyLevel) {
        this.writeConsistencyLevel = writeConsistencyLevel;
    }

    public ConsistencyLevel getReadConsistencyLevel() {
        return readConsistencyLevel;
    }

    public void setReadConsistencyLevel(ConsistencyLevel readConsistencyLevel) {
        this.readConsistencyLevel = readConsistencyLevel;
    }
}

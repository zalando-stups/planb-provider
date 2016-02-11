package org.zalando.planb.provider;

import com.google.common.base.MoreObjects;

public class ServiceSummary {

    private final String id;

    public ServiceSummary(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}

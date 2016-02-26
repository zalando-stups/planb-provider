package org.zalando.planb.provider;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.zalando.planb.provider.api.Client;

import java.util.Set;

public class ClientData {

    private final String clientSecretHash;
    private final Set<String> scopes;
    private final Boolean confidential;
    private final String createdBy;
    private final String lastModifiedBy;


    public ClientData(String clientSecretHash,
                      Set<String> scopes,
                      Boolean confidential,
                      String createdBy,
                      String lastModifiedBy) {
        this.clientSecretHash = clientSecretHash;
        this.scopes = scopes;
        this.confidential = confidential;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public Boolean isConfidential() {
        return confidential;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("scopes", scopes)
                .add("confidential", confidential)
                .add("createdBy", createdBy)
                .add("lastModifiedBy", lastModifiedBy)
                .toString();
    }

    public static Builder copyOf(Client client) {
        return new ClientData.Builder()
                .withClientSecretHash(client.getSecretHash())
                .withScopes(ImmutableSet.copyOf(client.getScopes()))
                .withConfidential(client.getIsConfidential());
    }

    public static class Builder {

        private String clientSecretHash;
        private Set<String> scopes;
        private Boolean confidential;
        private String createdBy;
        private String lastModifiedBy;

        public ClientData build() {
            return new ClientData(clientSecretHash, scopes, confidential, createdBy, lastModifiedBy);
        }

        public Builder withClientSecretHash(String clientSecretHash) {
            this.clientSecretHash = clientSecretHash;
            return this;
        }

        public Builder withScopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public Builder withConfidential(Boolean confidential) {
            this.confidential = confidential;
            return this;
        }

        public Builder withCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder withLastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return this;
        }
    }
}

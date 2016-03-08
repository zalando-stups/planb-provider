package org.zalando.planb.provider;

import com.google.common.collect.ImmutableSet;
import org.zalando.planb.provider.api.Client;

import java.util.Set;

public class ClientData {

    private final String clientSecretHash;
    private final Set<String> scopes;
    private final Boolean confidential;
    private final String name;
    private final String description;
    private final Set<String> redirectUris;
    private final String createdBy;
    private final String lastModifiedBy;


    public ClientData(String clientSecretHash,
                      Set<String> scopes,
                      Boolean confidential,
                      String name,
                      String description,
                      Set<String> redirectUris,
                      String createdBy,
                      String lastModifiedBy) {
        this.clientSecretHash = clientSecretHash;
        this.scopes = scopes;
        this.confidential = confidential;
        this.name = name;
        this.description = description;
        this.redirectUris = redirectUris;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public static Builder copyOf(Client client) {
        return new ClientData.Builder()
                .withClientSecretHash(client.getSecretHash())
                .withScopes(ImmutableSet.copyOf(client.getScopes()))
                .withConfidential(client.getIsConfidential())
                .withName(client.getName())
                .withDescription(client.getDescription())
                .withRedirectUris(ImmutableSet.copyOf(client.getRedirectUris()));
    }

    public static class Builder {

        private String clientSecretHash;
        private Set<String> scopes;
        private Boolean confidential;

        private String name;
        private String description;
        private Set<String> redirectUris;

        private String createdBy;
        private String lastModifiedBy;

        public ClientData build() {
            return new ClientData(clientSecretHash, scopes, confidential, name, description, redirectUris, createdBy, lastModifiedBy);
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

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }
        public Builder withRedirectUris(Set<String> redirectUris) {
            this.redirectUris = redirectUris;
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

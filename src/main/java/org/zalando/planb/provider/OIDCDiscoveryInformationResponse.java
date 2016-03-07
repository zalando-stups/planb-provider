package org.zalando.planb.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import java.util.List;

// see http://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
public class OIDCDiscoveryInformationResponse {

    public static final String AUTHORIZE_PATH = "/oauth2/authorize";
    public static final String KEYS_PATH = "/oauth2/connect/keys";

    private final String issuer = "PlanB";

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported = ImmutableList.of("code", "id_token");

    @JsonProperty("subject_types_supported")
    private List<String> subjectTypesSupported = ImmutableList.of("public");

    @JsonProperty("id_token_signing_alg_values_supported")
    private List<String> idTokenSigningAlgValuesSupported = ImmutableList.of(
            "RS256",
            "RS384",
            "RS512",
            "PS256",
            "PS384",
            "PS512",
            "ES256",
            "ES384",
            "ES512"
    );

    protected OIDCDiscoveryInformationResponse() {
    }

    public OIDCDiscoveryInformationResponse(String proto, String hostname) {
        authorizationEndpoint = proto + "://" + hostname + AUTHORIZE_PATH;

        jwksUri = proto + "://" + hostname + KEYS_PATH;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public List<String> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public List<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public List<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }
}

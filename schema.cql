CREATE SCHEMA provider
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};

-- IMPORTANT: for production, please use:
-- CREATE SCHEMA provider WITH replication = {'class': 'NetworkTopologyStrategy', 'eu-west': 3, 'eu-central': 3};

CREATE TABLE provider.client (
    client_id TEXT,                  -- OAuth 2 client ID
    realm TEXT,                      -- Data pool this entity belongs to
    client_secret_hash TEXT,         -- Base64-encoded Bcrypt hash of the client secret
    scopes SET<TEXT>,                -- scopes this client is allowed to request
    is_confidential BOOLEAN,         -- whether the client is confidential or not (non-confidential clients should only be allowed to use the implicit flow)
    PRIMARY KEY ((client_id), realm)
);

CREATE TABLE provider.user (
    username TEXT,              -- name of service user
    realm TEXT,                 -- Data pool this entity belongs to
    password_hashes SET<TEXT>,  -- set of Base64-encoded Bcrypt hashes
    scopes MAP<TEXT, TEXT>,     -- scopes and values for this user
    PRIMARY KEY ((username), realm)
);

CREATE TABLE provider.keypair (
    kid TEXT PRIMARY KEY,       -- JWK key ID
    realms SET<TEXT>,           -- for which realms to use this key, allows to tune the system for speed vs. size
    private_key_pem TEXT,       -- private key in PEM format
    algorithm TEXT,             -- JWK algorithm according to RFC 7518 3.1 https://tools.ietf.org/html/rfc7518#section-3.1
    valid_from INT              -- start date of key validity in seconds since epoch
);

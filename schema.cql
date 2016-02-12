CREATE KEYSPACE provider;
WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

CREATE TABLE provider.client (
	client_id TEXT PRIMARY KEY, -- OAuth 2 client ID
	client_secret_hash TEXT,    -- Base64-encoded Bcrypt hash of the client secret
	scopes SET<TEXT>,           -- scopes this client is allowed to request
	is_confidential BOOLEAN     -- whether the client is confidential or not (non-confidential clients should only be allowed to use the implicit flow)
);

CREATE TABLE provider.user (
	username TEXT PRIMARY KEY,  -- name of service user
	password_hashes SET<TEXT>,  -- set of Base64-encoded Bcrypt hashes
	scopes MAP<TEXT, TEXT>      -- scopes and values for this user
);

CREATE TABLE provider.keypair (
	kid TEXT PRIMARY KEY,       -- JWK key ID
	private_key_pem TEXT,       -- private key in PEM format
	public_key_jwk TEXT,        -- public key in JWK JSON format https://tools.ietf.org/html/rfc7517#section-4
	valid_from INT              -- start date of key validity in seconds since epoch
);

CREATE TABLE provider.keypair_archive (
	kid TEXT PRIMARY KEY,       -- JWK key ID
	public_key_jwk TEXT,        -- public key in JWK JSON format
	valid_from INT              -- (historic) start date of key validity in seconds since epoch
);

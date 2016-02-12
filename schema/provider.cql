CREATE KEYSPACE provider
WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

CREATE TABLE provider.client (
	client_id TEXT PRIMARY KEY,
	client_secret_hash TEXT,
	scopes SET<TEXT>,
	is_confidential BOOLEAN
);

CREATE TABLE provider.user (
	username TEXT PRIMARY KEY,
	password_hashes SET<TEXT>,
	scopes MAP<TEXT, TEXT>
);

CREATE TABLE provider.keypair (
	kid TEXT PRIMARY KEY,
	private_key_pem TEXT,
	public_key_jwk TEXT,
	valid_from INT
);

CREATE TABLE provider.keypair_archive (
	kid TEXT PRIMARY KEY ,
	public_key_jwk TEXT,
	valid_from INT
);

#!/usr/bin/env bash
set -e

CONTAINER_NAME="planb-provider-cassandra"

CONTAINER=$(docker ps -a -f "label=name=${CONTAINER_NAME}" -q)
if [ ! -z "$CONTAINER" ]; then
    echo "Removing container: ${CONTAINER_NAME}"
    docker rm -f ${CONTAINER}
fi

echo "Running container: ${CONTAINER_NAME}"
docker run --name ${CONTAINER_NAME} -l name=${CONTAINER_NAME} -d -p 9042:9042 cassandra:2.1

echo "Waiting 30 seconds for Cassandra to boot..."
sleep 30

echo "Creating Keyspace..."
docker run -i --link ${CONTAINER_NAME}:cassandra --rm cassandra:2.1 cqlsh cassandra < schema.cql

echo "Inserting sample data..."
# key pair
echo "INSERT INTO provider.keypair \
    (kid, realms, private_key_pem, algorithm, valid_from) VALUES \
    ('testkey', {'/services', '/customers', '/guest-customers'}, '$(cat src/test/resources/test-es384-secp384r1.pem)', 'ES384', $(date +"%s"));" > sample-data.cql

# confidential client
# client_id/client_secret: test0/test0
echo "INSERT INTO provider.client \
    (client_id, realm, client_secret_hash, is_confidential, scopes) VALUES \
    ('test0', '/services', '"'$2b$04$0PzwhGVD9MYyXd9sqtf/dOSgN1PC18dSWEliTQdUMT3hJztlvW3Em'"', true, {'uid'});" >> sample-data.cql

# non-confidential client
# client_id/client_secret: test1/test0
echo "INSERT INTO provider.client \
    (client_id, realm, client_secret_hash, is_confidential, scopes, default_scopes, redirect_uris) VALUES \
    ('test1', '/services', '"'$2b$04$0PzwhGVD9MYyXd9sqtf/dOSgN1PC18dSWEliTQdUMT3hJztlvW3Em'"', false, {'uid'}, {'uid'}, {'http://localhost:8080/callback'});" >> sample-data.cql

# user
# username/password: test0/test0
echo "INSERT INTO provider.user \
    (username, realm, password_hashes, scopes) VALUES \
    ('test0', '/services', { {password_hash: '"'$2b$04$0PzwhGVD9MYyXd9sqtf/dOSgN1PC18dSWEliTQdUMT3hJztlvW3Em'"', created: 1457044516, created_by: 'test'} }, {'uid': 'true'});" >> sample-data.cql

docker run -i --link ${CONTAINER_NAME}:cassandra --rm cassandra:2.1 cqlsh cassandra < sample-data.cql

rm -f sample-data.cql

echo "Done!"

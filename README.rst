===============
Plan B Provider
===============

.. image:: https://travis-ci.org/zalando/planb-provider.svg?branch=master
    :target: https://travis-ci.org/zalando/planb-provider

.. image:: https://codecov.io/github/zalando/planb-provider/coverage.svg?branch=master
    :target: https://codecov.io/github/zalando/planb-provider?branch=master

.. image:: https://readthedocs.org/projects/planb/badge/?version=latest
   :target: https://readthedocs.org/projects/planb/?badge=latest
   :alt: Documentation Status

This is a minimalistic `OpenID Connect Provider`_ that mainly supports the `Resource Owner Password Credentials Grant`_ to issue JWTs_ for *Service to Service* authentication.

More information is available in our `Plan B Documentation`_.

Building
========

Building the artifact and running all tests:

.. code-block:: bash

    $ ./mvnw verify

Find the executable jar in the target directory. Building a Docker image with the above artifact:

.. code-block:: bash

    $ sudo pip3 install scm-source
    $ scm-source
    $ docker build -t planb-provider .


Code Generation
===============

Java interfaces and classes for some REST APIs are auto-generated on build by swagger-codegen-maven-plugin. Find the
generated sources in target/generated-sources/swagger-codegen/.


Setting up Local Dev Environment
================================

Setup a local Cassandra
-----------------------

Automatically
~~~~~~~~~~~~~

.. code-block:: bash

    $ ./setup-dev-cassandra.sh

Manually
~~~~~~~~

.. code-block:: bash

    $ docker run --name dev-cassandra -d -p 9042:9042 cassandra:2.1

Insert schema (you might need to wait a few seconds for Cassandra to boot):

.. code-block:: bash

    $ docker run -i --link dev-cassandra:cassandra --rm cassandra:2.1 cqlsh cassandra < schema.cql

General cqlsh access to your dev instance:

.. code-block:: bash

    $ docker run -it --link dev-cassandra:cassandra --rm cassandra:2.1 cqlsh cassandra
      cqlsh> DESCRIBE TABLE provider.client; -- run some example query

Set up some signing keys and pipe resulting ``key.cql`` into cluster as well:

.. code-block:: bash

    $ echo "INSERT INTO provider.keypair
        (kid, realms, private_key_pem, algorithm, valid_from)
      VALUES
        ('testkey', {'/services', '/customers'}, '$(cat src/test/resources/test-es384-secp384r1.pem)', 'ES384', $(date +"%s"));" > key.cql
    $ docker run -i --link dev-cassandra:cassandra --rm cassandra:2.1 cqlsh cassandra < key.cql

Manually create our first test service user and client (password is "test0" for both):

.. code-block:: bash

    $ echo "INSERT INTO provider.client
        (client_id, realm, client_secret_hash, is_confidential, scopes)
      VALUES
        ('test0', '/services', '"'$2b$04$0PzwhGVD9MYyXd9sqtf/dOSgN1PC18dSWEliTQdUMT3hJztlvW3Em'"', true, {'uid'});" > testuser.cql
    $ echo "INSERT INTO provider.client
        (client_id, realm, client_secret_hash, is_confidential, scopes, default_scopes, redirect_uris)
      VALUES
        ('test1', '/services', '"'$2b$04$0PzwhGVD9MYyXd9sqtf/dOSgN1PC18dSWEliTQdUMT3hJztlvW3Em'"', false, {'uid'}, {'uid'}, {'http://localhost:8080/callback'});" >> testuser.cql
    $ echo "INSERT INTO provider.user
        (username, realm, password_hashes, scopes)
      VALUES
        ('test0', '/services', { {password_hash: '"'$2b$04$0PzwhGVD9MYyXd9sqtf/dOSgN1PC18dSWEliTQdUMT3hJztlvW3Em'"', created: 1457044516, created_by: 'test'} }, {'uid': 'true'});" >> testuser.cql
    $ docker run -i --link dev-cassandra:cassandra --rm cassandra:2.1 cqlsh cassandra < testuser.cql

Set up the following environment variables
------------------------------------------

.. code-block:: bash

    $ export OAUTH2_ACCESS_TOKENS=customerLogin=test             # fixed OAuth test token (unused)
    $ export TOKENINFO_URL=https://example.com/oauth2/tokeninfo  # required for /raw-sync REST API (unused here)

Run the application against your local Cassandra
------------------------------------------------

.. code-block:: bash

    $ java -jar target/planb-provider-1.0-SNAPSHOT.jar --cassandra.contactPoints="127.0.0.1"

Testing the Endpoints
=====================

Requesting a new JWT via Resource Owner Password Grant (using the example credentials inserted into Cassandra above):

.. code-block:: bash

    $ curl --silent -X POST -u test0:test0 -d "grant_type=password&username=test0&password=test0&scope=uid" \
         "http://localhost:8080/oauth2/access_token?realm=/services" | jq .

When requesting a new token via Implicit Flow, client will redirect user agent to the authorize endpoint. To test this, open the following link in your browser:

`http://localhost:8080/oauth2/authorize?redirect_uri=http://localhost:8080/callback&scope=uid&response_type=token&realm=/services&client_id=test1 <http://localhost:8080/oauth2/authorize?redirect_uri=http://localhost:8080/callback&scope=uid&response_type=token&realm=/services&client_id=test1>`_

Introducing credentials test0/test0 will redirect to the consent page. After accepting these, your agent should be redirected with the token as a parameter in the url, which should look like this:

`http://localhost:8080/callback?access_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ&token_type=Bearer&expires_in=28800&scope=uid&state= <http://localhost:8080/callback?access_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ&token_type=Bearer&expires_in=28800&scope=uid&state=>`_

The Authorization Code Grant flow is similar to the implicit flow, but we would be getting an authorization code instead of a token. To test this, open the following link in your browser:

`http://localhost:8080/oauth2/authorize?redirect_uri=http://localhost:8080/callback&scope=uid&response_type=code&realm=/services&client_id=test1 <http://localhost:8080/oauth2/authorize?redirect_uri=http://localhost:8080/callback&scope=uid&response_type=code&realm=/services&client_id=test1>`_

After login and accepting the consents, you will be redirected to the callback with a authorization code as parameter:

`http://localhost:8080/callback?code=ppdq9pjyaGxAF2YXXBnjOE9Hm4Dt-kXq&state=<http://localhost:8080/callback?code=ppdq9pjyaGxAF2YXXBnjOE9Hm4Dt-kXq&state=>`_

Redeeming the code for a token can be done as follows:

.. code-block:: bash

    $ curl --silent -X POST -d 'redirect_uri=http://localhost:8080/callback&code=<CODE_FROM_PREVIOUS_REQUEST>&grant_type=authorization_code&client_id=test1&client_secret=test1' "http://localhost:8080/oauth2/access_token" | jq .

Get the `OpenID Connect configuration discovery document`_:

.. code-block:: bash

    $ curl --silent http://localhost:8080/.well-known/openid-configuration | jq .


Retrieving all public keys (`set of JWKs`_) for verification:

.. code-block:: bash

    $ curl --silent http://localhost:8080/oauth2/connect/keys | jq .

Generating JWT Signing Keys
===========================

Use OpenSSL to generate JWT signing keys.

.. code-block:: bash

    $ openssl genrsa -out test-rs256-2048.pem 2048
    $ openssl ecparam -genkey -out test-es256-prime256v1.pem -name prime256v1
    $ openssl ecparam -genkey -out test-es384-secp384r1.pem -name secp384r1
    $ openssl ecparam -genkey -out test-es512-secp521r1.pem -name secp521r1

The resulting PEM file's contents must be stored in the ``private_key_pem`` column of the ``provider.keypair`` Cassandra table.


Configuration
=============

``TOKENINFO_URL``
    OAuth2 token info URL (can point to Plan B Token Info), this is used to secure the ``/raw-sync/`` REST endpoints.
``CUSTOMER_REALM_SERVICE_URL``
    Optional URL to Zalando customer service WSDL.
``ACCESS_TOKEN_URI``
    OAuth2 access token URL (can point to own endpoint), this is used to get OAuth tokens for upstream services.
``CASSANDRA_CONTACT_POINTS``
    Comma separated list of Cassandra cluster IPs.
``CASSANDRA_CLUSTER_NAME``
    Cassandra cluster name.
``API_SECURITY_RAW_SYNC_EXPR``
    Spring security expression, e.g. "#oauth2.hasScope('application.write_all_sensitive')"


.. _OpenID Connect Provider: https://openid.net/specs/openid-connect-core-1_0.html
.. _Implicit Grant Flow: https://tools.ietf.org/html/rfc6749#section-4.2
.. _Authorization Code Grant Flow: https://tools.ietf.org/html/rfc6749#section-4.1
.. _Resource Owner Password Credentials Grant: https://tools.ietf.org/html/rfc6749#section-4.3
.. _JWTs: https://tools.ietf.org/html/rfc7519
.. _Plan B Documentation: http://planb.readthedocs.org/
.. _OpenID Connect configuration discovery document: https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationResponse
.. _set of JWKs: https://tools.ietf.org/html/rfc7517#section-5

===============
Plan B Provider
===============

.. image:: https://travis-ci.org/zalando/planb-provider.svg?branch=master
    :target: https://travis-ci.org/zalando/planb-provider

.. image:: https://codecov.io/github/zalando/planb-provider/coverage.svg?branch=master
    :target: https://codecov.io/github/zalando/planb-provider?branch=master

This is a minimalistic OpenID Connect provider that currently only supports the Resource Owner Password Credentials
Grant to create JWTs.


Building
========

Setup the following env variable:

.. code-block:: bash

    $ export ACCESS_TOKEN_URI="https://example.com/oauth2/access_token"
    $ export CREDENTIALS_DIR="/meta/credentials"
    $ export OAUTH2_ACCESS_TOKENS=customerLogin=test

Building the artifact and running all tests:

.. code-block:: bash

    $ ./mvnw verify

Find the executable jar in the target directory. Building a Docker image with the above artifact:

.. code-block:: bash

    $ sudo pip3 install scm-source
    $ scm-source
    $ docker build -t planb-provider .


Code generation
===============

Java interfaces and classes for some REST APIs are auto-generated on build by swagger-codegen-maven-plugin. Find the
generated sources in target/generated-sources/swagger-codegen/.


Testing internal endpoints
==========================

.. code-block:: bash

    $ export CUSTOMER_LOGIN_REALM_URL="http://example.com/ws/customerService?wsdl"
    $ export CUSTOMER_LOGIN_TEST_USER="test"
    $ export CUSTOMER_LOGIN_TEST_PASSWORD="test"
    $ export CUSTOMER_LOGIN_TEST_CUSTOMER_NUMBER=12345
    $ ./mvnw verify -Pinternal


Setting up local dev environment
================================

Run a development Cassandra node:

.. code-block:: bash

    $ docker run --name dev-cassandra -d -p 9042:9042 cassandra:2.1

Insert schema:

.. code-block:: bash

    $ docker run -i --link dev-cassandra:cassandra --rm cassandra:2.1 cqlsh cassandra < schema.cql

General cqlsh access to your dev instance:

.. code-block:: bash

    $ docker run -it --link dev-cassandra:cassandra --rm cassandra:2.1 cqlsh cassandra

Set up some signing keys and pipe resulting keys.cql into cluster as well:

.. code-block:: bash

    $ echo "INSERT INTO provider.keypair
        (kid, realms, private_key_pem, algorithm, valid_from)
      VALUES
        ('testkey', {'/test', '/services'}, '$(cat src/test/resources/test-es384-secp384r1.pem)', 'ES384', $(date +"%s"));" > key.cql
    $ docker run -i --link dev-cassandra:cassandra --rm cassandra:2.1 cqlsh cassandra < key.cql

Run the application against you local Cassandra:

.. code-block:: bash

    $ java -jar target/planb-provider-1.0-SNAPSHOT.jar --cassandra.contactPoints="127.0.0.1"

Setting up some example keys
============================

.. code-block:: bash

    $ openssl genrsa -out test-rs256-2048.pem 2048
    $ openssl ecparam -genkey -out test-es256-prime256v1.pem -name prime256v1
    $ openssl ecparam -genkey -out test-es384-secp384r1.pem -name secp384r1
    $ openssl ecparam -genkey -out test-es512-secp521r1.pem -name secp521r1


Testing the endpoints
=====================

Requesting a new JWT:

.. code-block:: bash

    $ curl --silent -X POST -d "grant_type=password&username=foo&password=test&scope=uid" \
         "http://localhost:8080/oauth2/access_token?realm=/test" | jq .

Get the `OpenID Connect configuration discovery document`_:

.. code-block:: bash

    $ curl --silent http://localhost:8080/.well-known/openid-configuration | jq .


Retrieving all public keys (`set of JWKs`_) for verification:

.. code-block:: bash

    $ curl --silent http://localhost:8080/oauth2/v3/certs | jq .

.. _OpenID Connect configuration discovery document: https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationResponse
.. _set of JWKs: https://tools.ietf.org/html/rfc7517#section-5

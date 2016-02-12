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

testUser:  ${:test}
  testPassword:  ${:test}
  testCustomerNumber:  ${:12345}

Testing the endpoints
=====================

Requesting a new JWT:

.. code-block:: bash

    $ curl --silent -X POST -d "grant_type=password&username=foo&password=test&scope=uid" \
         "http://localhost:8080/oauth2/access_token?realm=/test" | jq .

Get the OpenID Connect configuration discovery document:

.. code-block:: bash

    $ curl --silent http://localhost:8080/oauth2/v3/certs | jq .


Retrieving all public keys for verification:

.. code-block:: bash

    $ curl --silent http://localhost:8080/oauth2/v3/certs | jq .

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

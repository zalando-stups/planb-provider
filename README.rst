===============
Plan B Provider
===============

.. image:: https://travis-ci.org/zalando/planb-provider.svg?branch=master
    :target: https://travis-ci.org/zalando/planb-provider

.. image:: https://codecov.io/github/zalando/planb-provider/coverage.svg?branch=master
    :target: https://codecov.io/github/zalando/planb-provider?branch=master

Building:

.. code-block:: bash

    $ ./mvnw verify

Docker Image
============

.. code-block:: bash

    $ ./mvnw package
    $ sudo pip3 install scm-source
    $ scm-source
    $ docker build -t planb-provider .

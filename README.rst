===============
Plan B Provider
===============

Building:

.. code-block:: bash

    $ ./mvnw clean install


To skip integration-test:

.. code-block:: bash

    $ ./mvnw clean install -DskipITs

Docker Image
============

.. code-block:: bash

    $ ./mvnw clean package
    $ sudo pip3 install scm-source
    $ scm-source
    $ docker build -t planb-provider .

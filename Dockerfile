FROM registry.opensource.zalan.do/stups/openjdk:8u66-b17-1-10

EXPOSE 8080

COPY target/planb-provider-0.1-SNAPSHOT.jar /planb-provider.jar
COPY target/scm-source.json /

CMD java $JAVA_OPTS $(java-dynamic-memory-opts) -jar /planb-provider.jar

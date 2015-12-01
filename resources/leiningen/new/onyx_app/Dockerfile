FROM java:openjdk-9-b88-jdk
MAINTAINER Excellent Person <fill@me.in>

ADD target/{{app-name}}-0.1.0-SNAPSHOT-standalone.jar /srv/{{app-name}}.jar

ADD script/run-peers.sh /srv/run-peers.sh

ENTRYPOINT ["/bin/sh", "/srv/run-peers.sh"]

FROM docker pull phusion/baseimage:0.9.17
MAINTAINER Michael Drogalis <mjd3089@rit.edu>

RUN apt-get update
RUN apt-get install -y software-properties-common
RUN add-apt-repository -y ppa:webupd8team/java

# Auto-accept the Oracle JDK license
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections

RUN apt-get install -y oracle-java8-installer

ADD target/{{app-name}}-0.1.0-SNAPSHOT-standalone.jar /srv/{{app-name}}.jar

ADD script/run-{{app-name}}.sh /srv/run-{{app-name}}.sh

ENTRYPOINT ["/bin/sh", "/srv/run-{{app-name}}.sh"]

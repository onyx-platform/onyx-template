FROM phusion/baseimage:0.9.17
MAINTAINER Excellent Person <fill@me.in>

RUN add-apt-repository -y ppa:webupd8team/java
RUN apt-get install -y software-properties-common
RUN apt-get update

# Auto-accept the Oracle JDK license
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections

RUN apt-get install -y oracle-java8-installer

RUN mkdir /etc/service/onyx_peer
RUN mkdir /etc/service/aeron

ADD target/{{app-name}}-0.1.0-SNAPSHOT-standalone.jar /srv/{{app-name}}.jar

ADD script/run_peers.sh /etc/service/onyx_peer/run
ADD script/run_aeron.sh /etc/service/aeron/run
CMD ["/sbin/my_init"]

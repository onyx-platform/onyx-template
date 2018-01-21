FROM frolvlad/alpine-oraclejdk8:full
MAINTAINER Gardner Vickers <gardner@vickers.me>

ADD https://github.com/just-containers/s6-overlay/releases/download/v1.11.0.1/s6-overlay-amd64.tar.gz /tmp/

RUN cp -r /tmp/etc/* /etc
RUN cp -r /tmp/usr/* /usr
RUN mv /tmp/init /

RUN tar xzf /tmp/s6-overlay-amd64.tar.gz -C /
RUN apk add --update libgcc libstdc++ bash

ADD scripts/run_media_driver.sh /etc/services.d/media_driver/run
ADD scripts/finish_media_driver.sh /etc/s6/media_driver/finish

ADD scripts/run_peer.sh /opt/run_peer.sh
ADD target/peer.jar /opt/peer.jar

ENTRYPOINT ["/init"]
EXPOSE 3196 3197 3198 40200
EXPOSE 40200
EXPOSE 40200/udp

CMD ["opt/run_peer.sh"]

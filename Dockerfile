# See: https://hub.docker.com/r/romraider/builder Overview for run details

FROM debian:13 AS rr_builder

RUN apt-get -y update && \
    apt-get -y upgrade && \
    apt-get -y install --no-install-recommends ant openjdk-21-jdk-headless && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

RUN useradd -ms /bin/bash romraider && \
    mkdir /home/romraider/RomRaider && \
    chown romraider:romraider /home/romraider/RomRaider

USER romraider:romraider
WORKDIR /home/romraider/RomRaider
RUN java -version && \
    echo "RomRaider build environment created."

# syntax=docker/dockerfile:1

FROM mcr.microsoft.com/playwright:v1.38.1-jammy
RUN apt-get update --allow-insecure-repositories && apt-get install -y git
RUN apt-get -y install openjdk-11-jre-headless
RUN apt-get -y install gettext-base
RUN apt-get -y install cgroupfs-mount
RUN cgroupfs-mount
RUN wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/bin/yq && chmod +x /usr/bin/yq
RUN install -m 0755 -d /etc/apt/keyrings
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
RUN chmod a+r /etc/apt/keyrings/docker.gpg
RUN echo "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
RUN apt-get update
RUN apt-get install -y --no-install-recommends docker.io docker-compose-plugin

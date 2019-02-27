FROM openjdk:8

ARG BRANCH_COMMON
ARG BRANCH_AS
ARG BRANCH_RAS

# Install.
RUN \
  sed -i 's/# \(.*multiverse$\)/\1/g' /etc/apt/sources.list && \
  apt-get update -y && \
  apt-get upgrade -y && \
  apt-get install -y build-essential && \
  apt-get install -y software-properties-common && \
  apt-get install -y byobu curl git htop man unzip vim wget maven && \
  apt-get install -y net-tools iputils-ping && \
  rm -rf /var/lib/apt/lists/*

# Set environment variables.
ENV HOME /root

# Define working directory.
WORKDIR /root

# Installing Common
RUN \
  git clone https://github.com/fogbow/common.git && \
  (cd common && git checkout $BRANCH_COMMON && mvn install -Dmaven.test.skip=true)

# Installing Authentication Service
RUN \
  git clone https://github.com/fogbow/authentication-service.git && \
  (cd authentication-service && git checkout $BRANCH_AS && mvn install -Dmaven.test.skip=true)

# Downloading Resource Allocation Service
RUN \
  git clone https://github.com/fogbow/resource-allocation-service.git && \
  (cd resource-allocation-service && git checkout $BRANCH_RAS)

# Define working directory.
WORKDIR /root/resource-allocation-service

# Generates the build number based on the commit checksum
RUN \
    (ras_build_number=$(git rev-parse --short 'HEAD') && \
    cd ../authentication-service && as_build_number=$(git rev-parse --short 'HEAD') && \
    cd ../common && common_build_number=$(git rev-parse --short 'HEAD') && \
    cd /root/resource-allocation-service && \ 
    echo "build_number=ras-$ras_build_number-as-$as_build_number-common-$common_build_number-(buzios)" > build)

RUN \
  mvn dependency:sources


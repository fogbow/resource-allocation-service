FROM openjdk:8

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

# Installing Manager
RUN \
  git clone https://github.com/fogbow/resource-allocation-service.git && \
  (cd resource-allocation-service && git checkout refactor-first-release && mvn install -Dmaven.test.skip=true)

# Define working directory.
WORKDIR /root/resource-allocation-service
  
RUN \
  mkdir -p src/main/resources/private
  
CMD bash bin/start-service > log.out 2> log.err && tail -f /dev/null

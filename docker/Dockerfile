FROM centos:7

COPY CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo
RUN yum install net-tools -y
RUN yum install iproute -y
RUN yum install tcpdump -y
RUN yum install wget -y

RUN wget https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_linux-x64_bin.tar.gz -o /opt/graalvm-jdk-21_linux-x64_bin.tar.gz
RUN tar -xvf /opt/graalvm-jdk-21_linux-x64_bin.tar.gz -C /opt/
ENV JAVA_HOME=/opt/graalvm-jdk-21.0.4+8.1
ENV PATH=$PATH:$JAVA_HOME/bin

RUN mkdir -p /app/
WORKDIR /app/
COPY sdwan-controller.jar /app/sdwan-controller.jar
COPY sdwan-node-bootstrap.jar /app/sdwan-node-bootstrap.jar

COPY entrypoint.sh /entrypoint.sh
RUN chmod 777 /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

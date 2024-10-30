```dockerfile
FROM centos:7

COPY graalvm-jdk-21_linux-x64_bin.tar.gz /opt/
RUN tar -xvf /opt/graalvm-jdk-21_linux-x64_bin.tar.gz -C /opt/
ENV JAVA_HOME=/opt/graalvm-jdk-21.0.4+8.1
ENV PATH=$PATH:$JAVA_HOME/bin

RUN mkdir -p /app/
COPY CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo
RUN yum install net-tools -y
RUN yum install iproute -y
RUN yum install tcpdump -y

WORKDIR /app/
```

```shell
docker run -d \
--name net-thunder \
--restart=always \
--privileged \
--mac-address 02:42:ac:11:00:02 \
-v $(pwd)/sdwan-node-bootstrap.jar:/app/sdwan-node-bootstrap.jar \
-v $(pwd)/application.yaml:/app/application.yaml \
net-thunder
```



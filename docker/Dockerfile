FROM jaspercloud/net-thunder-base

RUN mkdir -p /app/
WORKDIR /app/
COPY sdwan-controller.jar /app/sdwan-controller.jar
COPY sdwan-node-bootstrap.jar /app/sdwan-node-bootstrap.jar

COPY entrypoint.sh /entrypoint.sh
RUN chmod 777 /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

# docker build -f Dockerfile -t jaspercloud/net-thunder .

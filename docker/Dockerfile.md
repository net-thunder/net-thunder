docker镜像打包
```shell
docker build -t jaspercloud/net-thunder .
```

启动服务端
```shell
docker run -d \
--name net-thunder-server \
--restart=always \
--network=host \
-e server.port=11805 \
# controllerServer外网地址
-e sdwan.httpServer.controllerAddress=127.0.0.1:11805 \
-e sdwan.controllerServer.port=1800 \
# stunServer外网IP
-e sdwan.stunServer.bindHost=127.0.0.1 \
-e sdwan.stunServer.bindPort=3478 \
-e sdwan.relayServer.bindPort=2478 \
jaspercloud/net-thunder server
```

启动mesh端
```shell
docker run -d \
--privileged \
--name net-thunder-mesh \
--restart=always \
--mac-address 42:ac:bd:00:00:00 \
-e tenantId=default \
# controllerServer外网地址
-e controllerServer=127.0.0.1:11805 \
-e netMesh=true \
-e showVRouterLog=true \
-e showICELog=true \
-e showElectionLog=true \
-e showRouteRuleLog=true \
jaspercloud/net-thunder mesh
```



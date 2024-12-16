#!/bin/bash

case $1 in
    server)
        java -jar /app/sdwan-controller.jar
        ;;
    mesh)
        java -jar /app/sdwan-node-bootstrap.jar
        ;;
    *)
        echo "Invalid parameter: $1"
        exit 0
        ;;
esac

# Package
mvn package install -DskipTests -pl sdwan-node-bootstrap -am

# Collect Metadata with the Tracing Agent
java -jar -agentlib:native-image-agent=config-merge-dir=meta,config-write-period-secs=15,config-write-initial-delay-secs=10 sdwan-node-bootstrap.jar

# Build a Native Executable
mvn native:compile -DskipTests -pl sdwan-node-bootstrap -am -Pnative


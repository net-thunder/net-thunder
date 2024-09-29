https://dev.java/learn/jvm/tool/jpackage/

# build windows
jpackage --input sdwan-node-bootstrap/target/package/ --dest sdwan-node-bootstrap/target/ --name net-thunder --main-jar sdwan-node-bootstrap.jar --main-class org.springframework.boot.loader.launch.JarLauncher --win-dir-chooser --win-shortcut --verbose

# build osx
jpackage --input sdwan-node-bootstrap/target/package/ --dest sdwan-node-bootstrap/target/ --name net-thunder --main-jar sdwan-node-bootstrap.jar --main-class org.springframework.boot.loader.launch.JarLauncher --verbose

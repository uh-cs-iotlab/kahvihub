# Install and run the Kahvihub

Building the Kahvihub
---------------------
```
make embedded.test //Build
```

### Requirements for the install on the raspberry pi (with sqlite-jdbc)
Run anywhere before the build:
```
sudo apt-get install maven openjdk-7-jdk
git clone https://github.com/xerial/sqlite-jdbc.git
cd sqlite-jdbc
git checkout tags/3.8.11.2 -b 3.8.11.2
export JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:bin/javac::")
make
mvn install:install-file -Dfile=target/sqlite-jdbc-3.8.11.2.jar -DpomFile=pom.xml
```

Running Kahvihub
---------------------
```
make embedded.test.run
```


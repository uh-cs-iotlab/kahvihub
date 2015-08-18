# Install and run of Kahvihub

Gradle
-------
+ Install Gradle (tested with version 2.3)

Building the Kahvihub
---------------------
```
git clone https://github.com/uh-cs-iotlab/kahvihub.git
cd kahvihub
gradle build
cd ..
```

Running Kahvihub
---------------------
```
java -Djava.library.path=java-iothub-jni -jar kahvihub/build/libs/kahvihub-standalone-1.0.jar
```


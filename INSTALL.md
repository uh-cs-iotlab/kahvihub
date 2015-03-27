# Install and run of Kahvihub

Gradle
-------
+ Install Gradle (tested with version 2.3)
 
Building the JNI library for the IoT Hub
----------------------------------------
```
git clone https://github.com/uh-cs-iotlab/java-iothub-jni.git
cd java-iothub-jni
make
cd ..
```

Building the Java core library
------------------------------
```
git clone https://github.com/uh-cs-iotlab/java-iothub-core.git
cd java-iothub-core
gradle install (This will install the library to your local Maven repository)
cd ..
```

Building the Kahvihub
---------------------
```
git clone https://github.com/uh-cs-iotlab/kahvihub.git
cd kahvihub
gradle build
gradle jarStandalone
cd ..
```

Running Kahvihub
---------------------
```
java -Djava.library.path=java-iothub-jni -jar kahvihub/build/libs/kahvihub-standalone-1.0.jar
```


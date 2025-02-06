# LoadCode
Simple java based load testing tool.


## Getting Started.
1. Create any java project
2. Add the required library
Gradle:-
```groovy
implementation 'io.github.sapandang:loadcode:1.1'
implementation 'io.github.sapandang:Jrequests:1.2.3' //for sending requests
```

## Note
This library on its own cannot generate the request. It is dependent to the request.
Use any request library you want.


## Maven Repo
1. LoadCode: https://central.sonatype.com/artifact/io.github.sapandang/loadcode/overview
2. JRequests: https://central.sonatype.com/artifact/io.github.sapandang/Jrequests
3. JRequests Git: https://github.com/sapandang/JRequests

## Running the Load.
1. Create any class file define a method which you want to get executed.
2. start the load.
```java
//Core method for starting the load
 LoadTestExecutor tgroup = new LoadTestExecutor()
                        .setThreads(threads)
                        .setIterations(iterations)
                        .setRampUp(rampUp)
                        .setTestDuration(duration)
                        .addTest(testClass, "testScenario");
 tgroup.start();
```


package org.skd.loadcode;

public class Runner {


    public static void main(String[] args) throws Exception {
        LoadTestExecutor tgroup = new LoadTestExecutor()
                .setThreads(3)
                .setIterations(28)
                .setRampUp(0)  // Ramp-up over 10 sec
//                .setTestDuration(10)  // Run for 60 sec
//                .addTest(MyLoadTest.class, "testScenario");
                .addTest(MyLoadTest.class, "testcsv");

        tgroup.start();
    }

}

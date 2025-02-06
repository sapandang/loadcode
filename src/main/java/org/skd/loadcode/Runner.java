package org.skd.loadcode;

public class Runner {


    public static void run(Class<?> testClass,int threads,int iterations ,int rampUp,int duration) throws Exception {
                LoadTestExecutor tgroup = new LoadTestExecutor()
                        .setThreads(threads)
                        .setIterations(iterations)
                        .setRampUp(rampUp)
                        .setTestDuration(duration)
                        .addTest(testClass, "testScenario");
                tgroup.start();
    }

    public static void run(Class<?> testClass,int threads,int iterations ,int rampUp,int duration,String methodName) throws Exception {
        LoadTestExecutor tgroup = new LoadTestExecutor()
                .setThreads(threads)
                .setIterations(iterations)
                .setRampUp(rampUp)
                .setTestDuration(duration)
                .addTest(testClass, methodName);
        tgroup.start();
    }

    public static void runAsync(Class<?> testClass,int threads,int iterations ,int rampUp,int duration){
        new Thread(() -> {
            try {
                LoadTestExecutor tgroup = new LoadTestExecutor()
                        .setThreads(threads)
                        .setIterations(iterations)
                        .setRampUp(rampUp)
                        .setTestDuration(duration)
                        .addTest(testClass, "testScenario");
                tgroup.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
    public static void runAsync(Class<?> testClass,int threads,int iterations ,int rampUp,int duration,String methodName){
        new Thread(() -> {
            try {
                LoadTestExecutor tgroup = new LoadTestExecutor()
                        .setThreads(threads)
                        .setIterations(iterations)
                        .setRampUp(rampUp)
                        .setTestDuration(duration)
                        .addTest(testClass, methodName);
                tgroup.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }


    public static void main(String[] args) throws Exception {
        System.out.println("Hii, This is LoadCode");
    }

}

package org.skd.loadcode;



import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MyLoadTest {
    static RPMTimer rpmtimer2 = new RPMTimer(100);  // 100 RPM for request 3
    static CsvDataset csvDataset;

    static {
        try {
            csvDataset = new CsvDataset("payloads/feusers.csv",3,true,false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void testScenario(LoadTestExecutor tgroup) {
        System.out.println("Executing test: " + Thread.currentThread().getName());


        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void testcsv(LoadTestExecutor tgroup)
    {
        Map<String,String> row = csvDataset.getNextRow();
        List<Map<String, String>> rows = csvDataset.getNextBatch();
        if(row == null)
        {
            System.out.println("null");
        }
        System.out.println(""+rows);
    }


}
package se.kth.castor.deptrim.experiments;

public class PipelineRunner {
    public static void main(String[] args) throws Exception {
        DepcleanDataCollector.execute();
        DeptrimDataCollectorRQ1.execute();
        DeptrimDataCollectorRQ2.execute();
        DeptrimDataCollectorRQ3.execute();
    }
}

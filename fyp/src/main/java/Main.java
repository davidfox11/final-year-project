import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;

public class Main {

    public static void printProgress(int iterations, int n){
        int progressDashes = 100;
        double percentage = (iterations)*100/n;
        int hashes = 0;
        int hashBars = (int)percentage;
        int dashes = 100-hashBars;
        String strDashes = "-".repeat(dashes);
        //System.out.println(strDashes);
        String strHashes = "#".repeat(hashBars);
        //System.out.println(strHashes);

        System.out.printf("<%s%s> %.2f%s \r", strHashes, strDashes, percentage, "%");


    }

    public static void main(String[] args) throws ParseException, IOException {
        run("../tests/test30.txt");
    }

    public static void run(String resultsFile) throws ParseException, IOException {
        FileWriter fr = new FileWriter(new File(resultsFile));
        BufferedWriter br = new BufferedWriter(fr);
        /*
        Scheduler scheduler = new Scheduler();
        GenerateCustomers generator = new GenerateCustomers("../customers/customers1.csv", 50, 2, 1);
        generator.generate();
        scheduler.vehicles = scheduler.populateFleet(5, 5);
        scheduler.customers = scheduler.parseCustomers("../customers/customers1.csv");

        Plot myPlot = new Plot("Passenger Distribution",0,400,2,0,400,2);
        for (Customer customer : scheduler.customers){
            myPlot.setColor(scheduler.generateColor());
            myPlot.addPoint(customer.startPoint[0], customer.startPoint[1]);
            myPlot.setConnected(true);
            myPlot.addPoint(customer.endPoint[0], customer.endPoint[1]);
            myPlot.setConnected(false);
        }
         */

        // Set experiment - 20 runs of 5 minutes each
        int [][] changes = new int[][]{
                // increasing customer size
                new int[]{5, 0, 30, 5, 6, 3, 2},
                new int[]{5, 0, 40, 5, 6, 3, 2},
                new int[]{5, 0, 60, 5, 6, 3, 2},
                new int[]{5, 0, 70, 5, 6, 3, 2},
                // increasing time windows + sparsity
                new int[]{5, 0, 40, 5, 6, 2, 1},
                new int[]{5, 0, 40, 5, 6, 3, 2},
                new int[]{5, 0, 40, 5, 6, 4, 3},
                new int[]{5, 0, 40, 5, 6, 5, 4},
                // reducing number of vehicles
                new int[]{5, 0, 40, 5, 6, 3, 2},
                new int[]{5, 0, 40, 4, 6, 3, 2},
                new int[]{5, 0, 40, 3, 6, 3, 2},
                new int[]{5, 0, 40, 2, 6, 3, 2},
                // increasing vehicle capacity
                new int[]{5, 0, 40, 5, 6, 3, 2},
                new int[]{5, 0, 40, 5, 8, 3, 2},
                new int[]{5, 0, 40, 5, 10, 3, 2},
                new int[]{5, 0, 40, 5, 12, 3, 2},
        };
        printProgress(0, 32);
        int progressCount = 1;
        for (int j=40; j<=40; j+=10){
            int i = 2;
            String inputFile = String.format("customers%d.csv", i+1);
            IteratedLocalSearch ils = new IteratedLocalSearch(j, changes[i][1], inputFile, "../tests/test1.txt", changes[i][2], changes[i][3], changes[i][4], changes[i][5], changes[i][6]);
            try{
                ils.optimiseCost();
                br.write(ils.costMsg);
                System.out.println(ils.costMsg);
                printProgress(progressCount, 32);
                progressCount ++;
                br.write("");
                //ils.optimiseSatisfaction();
                //br.write(ils.satisfactionMsg);
                //System.out.println(ils.satisfactionMsg);
                printProgress(progressCount, 32);
                progressCount ++;
                br.write("");
            } catch (Exception e){
                e.printStackTrace();
                br.write("NPE. Next");
                continue;
            }



        }
        System.out.println("Done");

        br.close();
        fr.close();

    }
}

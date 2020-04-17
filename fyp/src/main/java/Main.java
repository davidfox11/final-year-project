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

    public static void main(String[] args) throws ParseException, IOException, InterruptedException {
        /* The following line runs the evaluations and saves them to a results file */
        //evaluate("../tests/test11b.txt");

        /* The following line is for testing different configurations of ILS */
        runIls();
    }

    public static void evaluate(String resultsFile) throws ParseException, IOException {
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
        for (int j=50; j<=50; j+=10){
            int i = 2;
            String inputFile = String.format("customers%d.csv", i+1);
            IteratedLocalSearch ils = new IteratedLocalSearch(j, changes[i][1], inputFile, "../tests/test1.txt", changes[i][2], changes[i][3], changes[i][4], changes[i][5], changes[i][6]);
            try{
                ils.objective = "score";
                ils.insertionMethod = 2;
                ils.swapMethod = new int[]{5, 10};
                ils.optimiseCost();
                br.write(ils.finalMsg);
                System.out.println(ils.finalMsg);
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

    /*
     * Run an instance of Iterated Local Search
     * The following parameters are required:
     * maxRunTime: The maximum time the algorithm should run for
     * resultsFile: The file where you would like results to be stored
     * customerCount: How many customer order sets to generate
     * fleetSize: the number of vehicles to dispatch
     * vehicleCapacity: The number of seats in each vehicle
     * maxTimeWindow: Maximum time between customer's pickup & drop-off time
     * sparsity: The maximum time between all pickup and drop-off time requests
     */
    public static void runIls() throws IOException, ParseException, InterruptedException {
        // Create an ILS instance here:
        IteratedLocalSearch ils = new IteratedLocalSearch(1, 0, "customers2.csv", "../tests/test1.txt", 40, 5, 6,3, 2);

        /*
         * In the following lines you can configure different aspects of the algorithm.
         * Each heuristic is number coded with their parameter as shown:
         * 1. firstFeasibleInsertion
         * 2. bestFeasibleInsertion
         * 3. one-to-one swap (maximum iterations)
         * 4. one-to-many swap (maximum iterations)
         * 5. large discard swap (removal constant)
         *
         * The default configuration for insertion is firstFeasibleInsertion
         * To change to bestFeasible, set insertionMethod to 2
         *
         * The default configuration for swaps is discard swap (10)
         * To change to one-to-one swap with a maximum iterations of 100,
         * swapMethod to new int{4, 100}
         */

        ils.insertionMethod = 2;
        ils.swapMethod = new int[]{4, 100};

        /*
         * You can set whether the algorithm will solve for static instance of the DARP
         * or real-time instances. By default, it will solve for static instances. To
         * change this, type:
         */
        ils.realTime = true;

        /*
         * If you would like to print routes to the terminal, set printRoutes = true.
         * By default, this setting is false
         */
        ils.printRoutes = true;

        /*
         * CHANGING OBJECTIVE
         * You can choose between minimising cost, maximise satisfaction
         * or minimising score.
         */
         ils.objective = "cost";


        /*
         * Using JComponent, a grid is created which maps out the state space of all passenger
         * pickup and drop-off requested locations. This is shown as soon as the algorithm begins
         * creating solutions. When each route is finished, the algorithm will output on the grid
         * the route that each vehicle has taken. By default, these grids are not shown. To show
         * them, type the following:
         */
        //ils.jComponent = true;

        /*
         * By default, the algorithm will create solution from the customer sets already made.
         * However, if you would like to generate new customer sets, you just need to write the
         * following:
         */
        //ils.generateNewOrders = true;

        ils.optimiseCost();
    }
}

import org.apache.commons.lang3.SerializationUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class IteratedLocalSearch {
    /*
     * for each iteration:
     * generate the routes
     * apply 2-opt search on each route
     * apply swaps between routes
     * for every route affected - apply 2-opt swap again
     * Maintain the route with the lowest cost
     */
    File results;
    String realTimeCustomers;
    int customerCount;
    FileWriter fr;
    BufferedWriter br;
    String costMsg = "";
    String satisfactionMsg = "";
    int n;
    int improvements = 0;
    String inputFile;
    List<SubGraph> bestAllocation;
    double lowestCost;
    double customerSatifaction = 0;
    Scheduler scheduler = new Scheduler();
    double keepFactor; //the percentage of passengers to keep from previous solution, 0-1
    List<SubGraph> firstAllocation = new ArrayList<>();

    public IteratedLocalSearch(int iterations, double keepFactor, String inputFile, String resultsFile, int customerCount, int fleetSize, int vehicleCapacity, int maxTimeWindow, int sparsity) throws IOException {
        this.n = iterations*60;
        //results = new File(resultsFile);
        String customers = "../customers/"+inputFile;
        //fr = new FileWriter(results);
        //br = new BufferedWriter(fr);
        //GenerateCustomers generator = new GenerateCustomers(customers, customerCount, maxTimeWindow, sparsity);
        //generator.generate();
        //REAL-TIME ONLY

        this.customerCount = customerCount;
        int preTime = customerCount/2;
        String preTimeCustomerFile = "../customers/preTime/"+inputFile;
        //GenerateCustomers generator = new GenerateCustomers(preTimeCustomerFile, preTime, maxTimeWindow, sparsity);
        //generator.generate();
        realTimeCustomers = "../customers/realTime/"+inputFile;
        //GenerateCustomers realTimeGenerator = new GenerateCustomers(realTimeCustomers, preTime, maxTimeWindow, sparsity);
        //realTimeGenerator.generate();


        this.keepFactor = keepFactor;
        scheduler.vehicles = scheduler.populateFleet(fleetSize, vehicleCapacity);
        //results = new File("../tests/test1.txt");
        costMsg += "************************************************************\n";
        // += "************************************************************\n";
        costMsg += String.format("NEW TEST - OPTIMIZING COST:\nNumber of iterations: %d\nVehicle Size: %d\nNumber of Vehicles: %d\nCustomer orders: %d orders from %s\nTime window: Max Time window: %d Sparsity: %d\n",
                iterations, vehicleCapacity, fleetSize, customerCount, inputFile, maxTimeWindow, sparsity);
        //satisfactionMsg += String.format("NEW TEST - OPTIMIZING CUSTOMER SATISFACTION:\nNumber of iterations: %d\nVehicle Size: %d\nNumber of Vehicles: %d\nCustomer orders: %d orders from %s\nTime window: Max Time window: %d Sparsity: %d\n",
        //        iterations, vehicleCapacity, fleetSize, customerCount, inputFile, maxTimeWindow, sparsity);
        //br.write(setup);
        // CONFIGURATION - INPUT FILE
        scheduler.customers = scheduler.parseCustomers(customers);
    }

    public List<SubGraph> optimiseCost() throws IOException, ParseException {
        int iterations = 0;
        long currentTime;
        double bestCustomerSatisfaction = 0;
        int maxIterations = 1;
        List<SubGraph> previousResult = new ArrayList<>();
        List<SubGraph> currentJourney;
        String progressBar;
        int progress;
        double initialCost;
        List<SubGraph> updatedJourney = new ArrayList<>();
        progressBar = "<----------------------------------------------------------------------------------------------------> 0.0%";
        //System.out.println(progressBar);
        long startTime = System.currentTimeMillis();
        currentTime = 0;
        try{
            while (currentTime < n){
                //printProgress(currentTime, n);
                if (iterations == 0 || keepFactor == 0){
                    currentJourney = getNewSolution();
                    if (iterations == 0){
                        //scheduler.printJourney(currentJourney);
                        initialCost = scheduler.score(currentJourney);
                        firstAllocation = currentJourney;
                        lowestCost = initialCost;
                        costMsg += ("Initial cost: "+ lowestCost);
                    }

                } else{
                    int customersToRemove = (int) (scheduler.customers.size()/(1-keepFactor));
                    //scheduler.printJourney(previousResult);
                    System.out.println("Removing "+customersToRemove+" customers");
                    //currentJourney = removeNCustomers(previousResult, customersToRemove);
                    currentJourney = scheduler.interRouteDiscardSwap(previousResult, 10);
                    scheduler.printJourney(currentJourney);
                    break;
                }

                updatedJourney = copyJourney(currentJourney);

                int numRoutes = updatedJourney.size();
                List<SubGraph> afterLocalSearch = new ArrayList<>();
                for (SubGraph sg : updatedJourney){
                    SubGraph newGraph = scheduler.twoOptSearchAlt(sg);
                    Route r = newGraph.adjustRoute();
                    newGraph.fillRoutes(r);
                    afterLocalSearch.add(newGraph);
                }
                updatedJourney = copyJourney(afterLocalSearch);
                double newCost = scheduler.getTotalJourneyCost(updatedJourney);

                // CONFIGURATION - INTER ROUTE SWAP COST
                updatedJourney = scheduler.interRouteDiscardSwap(updatedJourney,10);

                for (SubGraph sg : updatedJourney){
                    int originalSize = sg.route.succession.size();

                    List<Customer> newList = scheduler.removeDuplicates(sg.route.succession);
                    if (newList.size() != originalSize){
                        sg.route.succession = newList;
                        sg.route.generateRoute();
                        sg.fillRoutes(sg.route);
                    }

                }

                if (scheduler.score(updatedJourney) < lowestCost){
                    improvements ++;
                    bestAllocation = copyJourney(updatedJourney);
                    lowestCost = scheduler.score(updatedJourney);
                    //bestCustomerSatisfaction = scheduler.getCustomerSatisfaction(updatedJourney);
                }


                iterations ++;
                long newTime = System.currentTimeMillis();
                currentTime = (newTime-startTime)/1000;
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double minsPerIteration = (double)(totalTime/1000)/iterations;
            costMsg += String.format("\nFinal cost: %.2f\nCustomer Satisfaction: %.2f\nTotal of %d improvements after %d iterations\nAverage time for each iteration: %.2f seconds\n", lowestCost, bestCustomerSatisfaction, improvements, iterations, minsPerIteration);
/*
            String realTime = "Beginning real-time insertion....";
            scheduler.realTimeInsertion(realTimeCustomers, bestAllocation);
            double costAfterRt = scheduler.getTotalJourneyCost(updatedJourney);
            double satisfactionAfterRt = scheduler.getCustomerSatisfaction(updatedJourney);
            costMsg += String.format("Inserted %d real-time customers\nOverall cost: %.2f\nOverall satisfaction %.2f\n", customerCount/2, costAfterRt, satisfactionAfterRt);
            scheduler.printJourney(bestAllocation);
*/


        } catch (NullPointerException e){
            e.printStackTrace();
            costMsg += "NPE. End";


        }

        return bestAllocation;

    }

    public List<SubGraph> optimiseSatisfaction() throws IOException, ParseException {
        int iterations = 0;
        long currentTime;
        double bestCustomerSatisfaction = 0;
        double initialCustomerSatisfaction = 0;
        int maxIterations = 1;
        List<SubGraph> previousResult = new ArrayList<>();
        List<SubGraph> currentJourney;
        String progressBar;
        int progress;
        double initialCost = 0;
        List<SubGraph> updatedJourney = new ArrayList<>();
        //progressBar = "<----------------------------------------------------------------------------------------------------> 0.0%";
        //System.out.println(progressBar);
        long startTime = System.currentTimeMillis();
        currentTime = 0;
        try{
            while (currentTime < n){
                //printProgress(currentTime, n);
                if (iterations == 0 || keepFactor == 0){
                    currentJourney = getNewSolution();

                    if (iterations == 0){
                        initialCustomerSatisfaction = scheduler.getCustomerSatisfaction(currentJourney);
                        firstAllocation = currentJourney;
                        bestCustomerSatisfaction = initialCustomerSatisfaction;
                        //satisfactionMsg += ("Initial Customer Satisfaction: "+ bestCustomerSatisfaction);
                        //System.out.println("INITIAL JOURNEY");
                        //scheduler.printJourney(currentJourney);
                    }

                } else{
                    int customersToRemove = (int) (scheduler.customers.size()/(1-keepFactor));
                    currentJourney = removeNCustomers(previousResult, customersToRemove);
                }

                updatedJourney = copyJourney(currentJourney);
                int numRoutes = updatedJourney.size();
                List<SubGraph> afterLocalSearch = new ArrayList<>();
                for (SubGraph sg : updatedJourney){
                    SubGraph newGraph = scheduler.twoOptSearchAlt(sg);
                    Route r = newGraph.adjustRoute();
                    newGraph.fillRoutes(r);
                    afterLocalSearch.add(newGraph);
                }
                updatedJourney = copyJourney(afterLocalSearch);
                double newCost = scheduler.getTotalJourneyCost(updatedJourney);
                // CONFIGURATION - INTER ROUTE SWAP SATISFACTION
                updatedJourney = scheduler.interRouteDiscardSwapSatisfaction(updatedJourney,10);
                for (SubGraph sg : updatedJourney){
                    int originalSize = sg.route.succession.size();
                /*
                if (removeDuplicates(sg.route.succession)){
                    sg.route.generateRoute();
                    sg.fillRoutes(sg.route);
                }
                 */
                    List<Customer> newList = scheduler.removeDuplicates(sg.route.succession);
                    if (newList.size() != originalSize){
                        sg.route.succession = newList;
                        sg.route.generateRoute();
                        sg.fillRoutes(sg.route);
                    }

                }

                if (scheduler.getCustomerSatisfaction(updatedJourney) > bestCustomerSatisfaction){
                    improvements ++;
                    bestAllocation = copyJourney(updatedJourney);
                    lowestCost = scheduler.getTotalJourneyCost(updatedJourney);
                    bestCustomerSatisfaction = scheduler.getCustomerSatisfaction(updatedJourney);
                }

                iterations ++;
                long newTime = System.currentTimeMillis();
                currentTime = (newTime-startTime)/1000;
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double minsPerIteration = (double)(totalTime/1000)/iterations;
            //satisfactionMsg += String.format("\nFinal cost: %.2f\nCustomer Satisfaction: %.2f\nTotal of %d improvements after %d iterations\nAverage time for each iteration: %.2f seconds\n", lowestCost, bestCustomerSatisfaction, improvements, iterations, minsPerIteration);
            satisfactionMsg += bestCustomerSatisfaction+"\n";
            //FOR REAL TIME INSERTION
/*
            String realTime = "Beginning real-time insertion....";
            scheduler.realTimeInsertionSatisfaction(realTimeCustomers, bestAllocation);
            double costAfterRt = scheduler.getTotalJourneyCost(updatedJourney);
            double satisfactionAfterRt = scheduler.getCustomerSatisfaction(updatedJourney);
            satisfactionMsg += String.format("Inserted %d real-time customers\nOverall cost: %.2f\nOverall satisfaction %.2f\n", customerCount/2, costAfterRt, satisfactionAfterRt);

            satisfactionMsg += "************************************************************\n\n";
*/

        } catch (NullPointerException e){
            satisfactionMsg += "NPE. End";
            e.printStackTrace();
            return bestAllocation;
        }
        return bestAllocation;
    }

    public void printProgress(long currentTime, int n){
        int progressDashes = 100;
        double percentage = (double)(currentTime/1000)*100/n;
        int hashes = 0;
        int hashBars = (int)percentage;
        int dashes = 100-hashBars;
        String strDashes = "-".repeat(dashes);
        //System.out.println(strDashes);
        String strHashes = "#".repeat(hashBars);
        //System.out.println(strHashes);

        System.out.printf("<%s%s> %.2f%s \r", strHashes, strDashes, percentage, "%");

    }

    public Boolean removeDuplicates(List<Customer> customerList){
        System.out.println("NEW ITERATION");
        Boolean changed = false;
        Map<Customer, Integer> count = new HashMap<>();
        for (int i=0; i<customerList.size(); i++){
            Customer customer = customerList.get(i);
            if (count.containsKey(customer)){
                //System.out.printf("Customer %d: %d\n", customer.id, count.get(customer));
                if (count.get(customer) == 2){
                    //System.out.println("Removing customer "+customer.id);
                    customerList.remove(i);
                    changed = true;
                    i --;
                } else{
                    count.replace(customer, 2);
                    //System.out.printf("Customer %d is now at count %d\n", customer.id, count.get(customer));
                }
            } else {
                count.put(customer, 1);
                //System.out.printf("Customer %d is now at count %d\n", customer.id, count.get(customer));
            }
        }

        return changed;
    }

    public  List<SubGraph> copyJourney(List<SubGraph> journey){
        List<SubGraph> newJourney = new ArrayList<>();
        for (SubGraph sg : journey){
            newJourney.add(SerializationUtils.clone(sg));
        }

        return  newJourney;
    }

    public List<SubGraph> getNewSolution(){
        List<Route> routes = scheduler.generateSolution();
        Graph graph = new Graph(scheduler.customers, scheduler.vehicles);
        for (int i=0;i<routes.size();i++){
            SubGraph sg = graph.createSubGraph(routes.get(i));
            sg.fillRoutes(routes.get(i));
            graph.routes.add(sg);
        }

        return graph.routes;
    }

    public List<SubGraph> removeNCustomers(List<SubGraph> journey, int n){
        List<SubGraph> newOrder = new ArrayList<>();
        Random rand = new Random();
        List<SubGraph> cannotRemoveMore = new ArrayList<>();

        for (SubGraph sg : journey){
            newOrder.add(SerializationUtils.clone(sg));
        }
        //remove N customers at random from sg and add to the discard pile
        while (n>0 && cannotRemoveMore.size() < journey.size()){
            //randomly select route
            int r1 = rand.nextInt((newOrder.size() - 1) + 1);
            SubGraph currentRoute = newOrder.get(r1);
            if (cannotRemoveMore.contains(currentRoute)) continue;
            if (currentRoute.getSize() <= 2){
                cannotRemoveMore.add(currentRoute);
                continue;
            }
            //randomly select customer
            int r2 = rand.nextInt((currentRoute.getSize() - 2) + 1);
            int[] firstRoutePassengerLocations = scheduler.getCustomerVertexes(currentRoute, r2);
            Vertex[] vertexPairs = currentRoute.removePassenger(firstRoutePassengerLocations[0], firstRoutePassengerLocations[1]);
            scheduler.discardList.add(vertexPairs[0].customer);
            //System.out.printf("Customer %d added to discard pile\n", vertexPairs[0].customer.id);
            n --;
        }

        for (Iterator<Customer> iterator = scheduler.discardList.iterator(); iterator.hasNext(); ) {
            Customer currentCustomer = iterator.next();
            int i = 0;
            int r1 = rand.nextInt((newOrder.size() - 1) + 1);
            while (i < newOrder.size()){
                SubGraph sg = newOrder.get(r1);
                List<Customer> currentCustomerList = scheduler.insertCustomer(currentCustomer, sg.route.succession, sg.route.vehicle.capacity);
                if (currentCustomerList != null){
                    System.out.printf("Successfully added customer %d to route %s\n", currentCustomer.id, sg.id);
                    sg.route.succession = currentCustomerList;
                    sg.fillRoutes(sg.route);
                    //discardList.remove(currentCustomer);
                    iterator.remove();
                    break;
                } else{
                    if (r1 == newOrder.size()-1){
                        r1 = 0;
                    } else {
                        r1 ++;
                    }
                }
                i ++;
            }

        }

        return newOrder;
    }

    public void printRoutes() throws InterruptedException {
        Plot myPlot = new Plot("Vehicle Routes",0,400,2,0,400,2);
        for (SubGraph sg : bestAllocation){
            myPlot.setColor(scheduler.generateColor());
            Vertex curent = sg.head;
            myPlot.addPoint(curent.location[0], curent.location[1]);
            while (sg.getNextVertex(curent) != null){
                curent = sg.getNextVertex(curent);
                myPlot.setConnected(true);
                myPlot.addPoint(curent.location[0], curent.location[1]);
                TimeUnit.MILLISECONDS.sleep(500);
            }
            myPlot.clearThePlot();
            TimeUnit.SECONDS.sleep(5);
            myPlot.setConnected(false);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        //int iterations, double keepFactor, String inputFile, int customerCount, int fleetSize, int vehicleCapacity
        IteratedLocalSearch ils = new IteratedLocalSearch(10, 0, "customers2.csv", "../tests/test11.txt", 30, 5, 6, 3, 2);
        Plot myPlot = new Plot("Passenger Distribution",0,400,2,0,400,2);
        for (Customer customer : ils.scheduler.customers){
            myPlot.setColor(ils.scheduler.generateColor());
            myPlot.addPoint(customer.startPoint[0], customer.startPoint[1]);
            myPlot.setConnected(true);
            myPlot.addPoint(customer.endPoint[0], customer.endPoint[1]);
            myPlot.setConnected(false);
        }
        long startTime = System.currentTimeMillis();
        List<SubGraph> newRoute = ils.optimiseCost();
        ils.scheduler.printJourney(newRoute);
        //System.out.println(ils.costMsg);
        ils.printRoutes();
        long stopTime = System.currentTimeMillis();
        //System.out.println("Time taken: "+(stopTime - startTime)/1000+" seconds");
    }
}

import org.apache.commons.lang3.SerializationUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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
    FileWriter fr;
    BufferedWriter br;
    int n;
    int improvements = 0;
    String inputFile;
    List<SubGraph> bestAllocation;
    double lowestCost;
    Scheduler scheduler = new Scheduler();
    double keepFactor; //the percentage of passengers to keep from previous solution, 0-1
    List<SubGraph> firstAllocation = new ArrayList<>();

    public IteratedLocalSearch(int iterations, double keepFactor, String inputFile, int customerCount, int fleetSize, int vehicleCapacity) throws IOException {
        this.n = iterations;
        results = new File(inputFile);
        fr = new FileWriter(results);
        br = new BufferedWriter(fr);
        GenerateCustomers generator = new GenerateCustomers(inputFile, customerCount);
        generator.generate();
        this.keepFactor = keepFactor;
        scheduler.vehicles = scheduler.populateFleet(fleetSize, vehicleCapacity);
        results = new File("../tests/test1.txt");
        br.write("************************************************************");
        String setup = String.format("NEW TEST:\nNumber of iterations: %d\nVehicle Size: %d\nNumber of Vehicles: %d\nCustomer orders: %d orders from %s",
                iterations, vehicleCapacity, fleetSize, customerCount, inputFile);
        br.write(setup);
        scheduler.customers = scheduler.parseCustomers(inputFile);
    }

    public List<SubGraph> optimise() throws IOException {
        int iterations = 0;
        int maxIterations = 1;
        List<SubGraph> previousResult = new ArrayList<>();
        List<SubGraph> currentJourney;
        String progressBar;
        int progress;
        double initialCost = 0;
        List<SubGraph> updatedJourney = new ArrayList<>();
        progressBar = "<----------------------------------------------------------------------------------------------------> 0.0%";
        System.out.println(progressBar);
        while (iterations < n){
            //printProgress(iterations, n);
            if (iterations == 0 || keepFactor == 0){
                currentJourney = getNewSolution();
                if (iterations == 0){
                    scheduler.printJourney(currentJourney);
                    initialCost = scheduler.getTotalJourneyCost(currentJourney);
                    firstAllocation = currentJourney;
                    lowestCost = initialCost;
                    br.write("Initial cost: "+ lowestCost);
                    System.out.println("INITIAL JOURNEY");
                    scheduler.printJourney(currentJourney);
                }

            } else{
                int customersToRemove = (int) (scheduler.customers.size()/(1-keepFactor));
                currentJourney = removeNCustomers(previousResult, customersToRemove);
            }

            //System.out.println("Current journey cost: "+ scheduler.getTotalJourneyCost(currentJourney));
            //System.out.println("Beginning Local Search Within Vehicles...");
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
            //System.out.println("Local search completed");
            double newCost = scheduler.getTotalJourneyCost(updatedJourney);
            //System.out.printf("Score has improved from %.2f to %.2f\n", scheduler.getTotalJourneyCost(currentJourney), newCost);

            //System.out.println("Beginning swaps between vehicles...");
            updatedJourney = scheduler.interRouteDiscardSwap(updatedJourney, 5);
            //System.out.println("Inter route search completed");
            //System.out.printf("Score has improved from %.2f to %.2f\n", newCost, scheduler.getTotalJourneyCost(updatedJourney));

            //System.out.println("Removing from final journey");
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

            /*
            scheduler.printJourney(updatedJourney);
            System.out.println("Performing local search on changed vehicles...");
            List<SubGraph> afterLocalSearch1 = new ArrayList<>();
            for (SubGraph sg : updatedJourney){
                SubGraph newGraph = scheduler.twoOptSearchAlt(sg);
                Route r = newGraph.adjustRoute();
                newGraph.fillRoutes(r);
                afterLocalSearch1.add(newGraph);
            }
            updatedJourney = copyJourney(afterLocalSearch1);
             */

            //System.out.println("UPDATED JOURNEY");
            //scheduler.printJourney(updatedJourney);
            //System.out.printf("In this iteration, score has improved from %.2f to %.2f\n", initialCost, scheduler.getTotalJourneyCost(updatedJourney));

            if (scheduler.getTotalJourneyCost(updatedJourney) < lowestCost){
                improvements ++;
                bestAllocation = copyJourney(updatedJourney);
                lowestCost = scheduler.getTotalJourneyCost(updatedJourney);
            }

            iterations ++;
        }

        scheduler.printJourney(bestAllocation);
        System.out.println("Initial Cost: "+ initialCost);
        System.out.println("Final Cost: "+ lowestCost);
        String msg = String.format("Algorithm finished\nFinal cost: %d\nTotal of %d improvements after %d iterations", lowestCost, improvements, maxIterations);
        br.write(msg);
        br.close();
        fr.close();
        return bestAllocation;
    }

    public void printProgress(int iterations, int n){
        int progressDashes = 100;
        double percentage = (double)iterations*100/n;
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

    public static void main(String[] args) throws IOException {
        //int iterations, double keepFactor, String inputFile, int customerCount, int fleetSize, int vehicleCapacity
        IteratedLocalSearch ils = new IteratedLocalSearch(1000, 0, "customers1.csv", 30, 5, 5);
        long startTime = System.currentTimeMillis();
        ils.optimise();
        long stopTime = System.currentTimeMillis();
        System.out.println("Time taken: "+(stopTime - startTime)/1000+" seconds");
    }
}

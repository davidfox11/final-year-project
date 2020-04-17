import org.apache.commons.lang3.SerializationUtils;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class Scheduler {
    List<Customer> customers;
    List<Vehicle> vehicles;
    List<Customer> discardList;
    int twoOptComparisons;
    int twoOptImprovements;
    int interRouteImprovements;
    List<SubGraph> interRouteSwapChanges = new ArrayList<>();

    public Scheduler(){
        discardList = new ArrayList<>();
    }

    public List<Route> generateSolution(int insertionMethod){
        Comparator<Customer> priorityComparison = Comparator.comparing(Customer::getPriority);
        PriorityQueue<Customer> priorityQueue = new PriorityQueue<>( priorityComparison );
        List<Route> routes = new ArrayList<>();

        // Assign a random priority to each customer
        for (Customer customer : customers){
            customer.priority = customer.randomPriority(0, 100);
            priorityQueue.add(customer);
        }

        // fill each vehicle
        for (int i=0;i<vehicles.size();i++){
            List<Customer> addAfterVehicleAssignment = new ArrayList<>();
            //System.out.println("Filling Vehicle " + vehicles.get(i).id);
            vehicles.get(i).passengers = new ArrayList<>();

            while (!priorityQueue.isEmpty()){
                Customer nextCustomer = priorityQueue.poll();
                List<Customer> newPassengerList = insertCustomer(nextCustomer, vehicles.get(i).passengers, vehicles.get(i).capacity, insertionMethod);
                if (newPassengerList != null){
                    vehicles.get(i).passengers = newPassengerList;
                    continue;
                } else if (newPassengerList == null && i<vehicles.size()-1){
                    nextCustomer.priority = nextCustomer.randomPriority(0, 100);
                    //System.out.printf("Customer %d not feasible. Placing back into priority queue with new priority %d\n", nextCustomer.id, nextCustomer.priority);
                    if (!addAfterVehicleAssignment.contains(nextCustomer)){
                        addAfterVehicleAssignment.add(nextCustomer);
                    }
                    continue;
                } else{
                    //System.out.printf("Customer %d cannot be assigned to any vehicle. Adding to discard pile\n", nextCustomer.id);
                    discardList.add(nextCustomer);
                }
            }
            Route route = new Route(vehicles.get(i).id, vehicles.get(i).passengers, vehicles.get(i));
            if (route.succession.size() > 0){
                route.generateRoute();
                routes.add(route);
            }
            for (Customer customer : addAfterVehicleAssignment){
                customer.assignmentAttempts ++;
                if (!priorityQueue.contains(customer)) priorityQueue.add(customer);
                continue;
            }
        }
        return routes;
    }

    public List<Customer> insertCustomer(Customer customer, List<Customer> currentPassengers, int capacity, int insertionMethod){
        /*
        size:
        1 - ..A .A. A..
        2 - ..AB .A.B A..B A.B. AB..
        3 - ..ABC .A.BC A..BC A.B.C AB..C AB.C. ABC..
        4 - ..ABCD .A.BCD A..BCD A.B.CD AB..CD AB.C.D ABC..D ABC.D. ABCD..

        for every passenger currently in list:
            2 before
            1 before 1 after
            2 after (only if final element)

            size: 2
            options: 00,02,12,13,23
         */
        List<int[]> customerPositions = new ArrayList<>();
        if (currentPassengers.size() == 0){
            currentPassengers.add(customer);
            currentPassengers.add(customer);
            return currentPassengers;
        } else {
            for (int i = 0; i < currentPassengers.size(); i++) {
                customerPositions.add(new int[]{i, i + 1});
                customerPositions.add(new int[]{i, i + 2});
                if (i == currentPassengers.size() - 1) customerPositions.add(new int[]{i + 1, i + 2});
            }
        }

        //System.out.println("Checking feasibility of possible positions for customer " + customer.id);
        //System.out.println("....................");
        //System.out.println("....................\n");
        // CONFIGURATION - SOLUTION GENERATION
        int[] feasibleRoute = null;
        if (insertionMethod == 1){
            feasibleRoute = getFirstFeasible(currentPassengers, customer, customerPositions, capacity);
        } else if (insertionMethod == 2){
            feasibleRoute = getBestFeasible(currentPassengers, customer, customerPositions, capacity);
        }
        if (feasibleRoute != null){
            //System.out.printf("Best feasible indexes for customer %d: {%d, %d}\n", customer.id, bestFeasibleRoute[0], bestFeasibleRoute[1]);
            if (feasibleRoute[0] > currentPassengers.size()){
                currentPassengers.add(customer);
            } else{
                currentPassengers.add(feasibleRoute[0], customer);
            }
            if (feasibleRoute[1] > currentPassengers.size()){
                currentPassengers.add(customer);
            } else{
                currentPassengers.add(feasibleRoute[1], customer);
            }

            return currentPassengers;
        } else{
            // no feasible allocation, place passenger in queue with new priority
            return null;
        }
    }

    public int[] getFirstFeasible(List<Customer> passengers, Customer customer, List<int[]> customerPositions, int capacity){
        for (int[] order : customerPositions){
            passengers.add(order[0], customer);
            passengers.add(order[1], customer);

            Route route = new Route(100, passengers, new Vehicle(100, new int[]{50,50}, capacity));
            route.generateRoute();

            if (isFeasible(route)){
                deleteRoute(route);
                passengers.remove(customer);
                passengers.remove(customer);

                return order;
            }
            passengers.remove(customer);
            passengers.remove(customer);

            deleteRoute(route);
        }
        return null;
    }

    public int[] getBestFeasible(List<Customer> passengers, Customer customer, List<int[]> customerPositions, int capacity){
        //time limit - 20 seconds
        long currentTime;
        int deadline = 20*1000;
        long startTime = System.currentTimeMillis();
        double lowestCost = 100000;
        int[] bestFeasible = new int[]{-1,-1};
        //System.out.printf("Total number of allocations for customer %d: %d\n", customer.id, customerPositions.size());
        currentTime = System.currentTimeMillis();
        for (int[] order : customerPositions){
            if (currentTime-startTime >= deadline){
                if (bestFeasible[0] == -1) {
                    return null;
                }
                return bestFeasible;
            }
            if (order[0] > passengers.size()){
                passengers.add(customer);
            } else {
                passengers.add(order[0], customer);
            }
            if (order[1] > passengers.size()){
                passengers.add(customer);
            } else {
                passengers.add(order[1], customer);
            }

            Route route = new Route(100, passengers, new Vehicle(100, new int[]{50,50}, capacity));
            route.generateRoute();

            if (isFeasible(route)){
                double cost = route.getCost();
                if (cost < lowestCost){
                    lowestCost = cost;
                    bestFeasible = order;
                }
                deleteRoute(route);
                passengers.remove(customer);
                passengers.remove(customer);
            }
            passengers.remove(customer);
            passengers.remove(customer);

            deleteRoute(route);
        }

        if (bestFeasible[0] == -1) {
            return null;
        }

        return bestFeasible;

    }

    public void deleteRoute(Route route){
        route.id = 0;
        route.succession = null;
        route.vehicle = null;
        route.beginningOfService = null;
        route.endOfService = null;
        route.collectedCustomers = null;
    }

    public Boolean isFeasible(Route route){
        /*
         * A route is feasible if:
         * pickup time is after first window
         * drop-off time is before last window
         * passenger is not picked up before being dropped off
         * wait time does not exceed 100 minutes
         * travel time does not exceed 150 minutes
         */
        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("HH:mm:ss");
        for (Customer passenger : route.succession){
            // check time windows
            if (passenger.pickupTime.isBefore(passenger.timeWindow[0])){
                return false;
            }
            if (passenger.dropoffTime.isAfter(passenger.timeWindow[1])){
                return false;
            }
            if (route.capacityExceeded(route.vehicle.capacity)){
                return false;
            }

            /*
            if (route.getEndOfService().isAfter(route.vehicle.endTime)){
                System.out.println("Route has reached end of service");
                return false;
            }
             */
        }
        return true;
    }

    public List<SubGraph> interRouteDiscardSwap(List<SubGraph> routes, int n, String objective, int insertionMethod){
        /*
         * Remove n passengers from random vehicles and add to discard pile
         * Go through discard pile - for each passenger, loop through routes
         * (randomly) and try to insert passenger into route. If they cannot
         * be inserted, they can remain in the discard pile.
         */
        int maxIterations = 10;
        List<SubGraph> bestOrder = routes;
        int iterations = 0;
        int improvements = 0;
        List<SubGraph> newOrder;
        Random rand = new Random();
        List<SubGraph> changed = new ArrayList<>();
        List<SubGraph> cannotRemoveMore = new ArrayList<>();

        while (iterations<maxIterations){
            List<SubGraph> testOrder = new ArrayList<>();
            for (SubGraph sg : bestOrder){
                testOrder.add(SerializationUtils.clone(sg));
            }
            //remove n passengers from random graphs
            while (n>0 && cannotRemoveMore.size() < bestOrder.size()){
                //randomly select route
                int r1 = rand.nextInt((testOrder.size() - 1) + 1);
                SubGraph currentRoute = testOrder.get(r1);
                if (cannotRemoveMore.contains(currentRoute)) continue;
                if (currentRoute.getSize() <= 2){
                    cannotRemoveMore.add(currentRoute);
                    continue;
                }
                //randomly select customer
                int r2 = rand.nextInt((currentRoute.getSize() - 2) + 1);
                int[] firstRoutePassengerLocations = getCustomerVertexes(currentRoute, r2);
                Vertex[] vertexPairs = currentRoute.removePassenger(firstRoutePassengerLocations[0], firstRoutePassengerLocations[1]);
                discardList.add(vertexPairs[0].customer);
                //System.out.printf("Customer %d added to discard pile\n", vertexPairs[0].customer.id);
                n --;
            }

            //remove duplicates from discard pile
            List<Customer> alreadyPresent = new ArrayList<>();
            for (int i=0;i<discardList.size();i++){
                if (alreadyPresent.contains(discardList.get(i))){
                    discardList.remove(i);
                    i --;
                } else{
                    alreadyPresent.add(discardList.get(i));
                }
            }

            List<SubGraph> finalList = new ArrayList<>();
            for (Iterator<Customer> iterator = discardList.iterator(); iterator.hasNext(); ) {
                Customer currentCustomer = iterator.next();
                int i = 0;
                int r1 = rand.nextInt((testOrder.size() - 1) + 1);
                while (i < testOrder.size()){
                    SubGraph sg = testOrder.get(r1);
                    List<Customer> currentCustomerList = insertCustomer(currentCustomer, sg.route.succession, sg.route.vehicle.capacity, insertionMethod);
                    if (currentCustomerList != null){
                        //System.out.printf("Successfully added customer %d to route %s\n", currentCustomer.id, sg.id);
                        currentCustomerList = removeDuplicates(currentCustomerList);
                        testOrder.remove(sg);
                        //System.out.printf("Route %d size: %d", sg.id, currentCustomerList.size());
                        Route r = new Route(sg.route.id, currentCustomerList, sg.route.vehicle);
                        //sg.route.succession = currentCustomerList;
                        r.generateRoute();
                        //System.out.printf("Route %d size: %d", r.id, r.succession.size());
                        SubGraph sgNew = new SubGraph(sg.id);
                        sgNew.fillRoutes(r);
                        //System.out.printf("Route %d graph size: %d, list size: %d", sgNew.id, sgNew.getSize(), sgNew.route.succession.size());
                        testOrder.add(sgNew);

                        /*
                        sg.route.succession = currentCustomerList;
                        System.out.println(sg.route.succession.size());
                        sg.route.generateRoute();
                        System.out.println(sg.route.succession.size());
                        sg.fillRoutes(sg.route);
                        System.out.println(sg.route.succession.size());
                        System.out.println(sg.printGraph());
                         */

                        changed.add(sg);
                        //discardList.remove(currentCustomer);
                        iterator.remove();
                        //System.out.println("Discard list size: "+discardList.size());
                        break;
                    } else{
                        if (r1 == testOrder.size()-1){
                            r1 = 0;
                        } else {
                            r1 ++;
                        }
                    }
                    i ++;
                }
                interRouteSwapChanges = changed;
            }

            if (checkForImprovement(bestOrder, testOrder, objective)){
                improvements ++;
                bestOrder = testOrder;
            }
            iterations ++;
        }
        //System.out.println("Improvements made: " + improvements);
        interRouteImprovements = improvements;

        return bestOrder;
    }

    public Boolean checkForImprovement(List<SubGraph> originalJourney, List<SubGraph> updatedJourney, String objective){
        double originalCost = getTotalJourneyCost(originalJourney);
        double updatedCost = getTotalJourneyCost(originalJourney);
        double originalSatisfaction = getCustomerSatisfaction(originalJourney);
        double updatedSatisfaction = getCustomerSatisfaction(originalJourney);
        double originalScore = score(originalJourney);
        double updatedScore = score(originalJourney);
        Map<String, double[]> methods = new HashMap<>();
        methods.put("cost", new double[]{originalCost, updatedCost});
        methods.put("satisfaction", new double[]{originalSatisfaction, updatedSatisfaction});
        methods.put("score", new double[]{originalScore, updatedScore});

        if (objective.equals("satisfaction")){
            return (methods.get(objective)[1] > methods.get(objective)[0]);
        }

        return (methods.get(objective)[1] < methods.get(objective)[0]);
    }

    public List<Customer> removeDuplicates(List<Customer> customerList){
        Map<Customer, Integer> count = new HashMap<>();
        for (int i=0; i<customerList.size(); i++){
            Customer customer = customerList.get(i);
            if (count.containsKey(customer)){
                if (count.get(customer) == 2){
                    //System.out.println("Removing customer "+customer.id);
                    customerList.remove(i);
                    i --;
                } else{
                    count.replace(customer, 2);
                }
            } else {
                count.put(customer, 1);
            }
        }

        return customerList;
    }

    public double getTotalJourneyCost(List<SubGraph> journey){
        double cost = 0;
        if (journey.size() == 0) return 0;
        for (SubGraph sg : journey){
            cost += sg.getCost();
        }
        cost += getDiscardListCost();

        return cost;
    }

    public double getDiscardListCost(){
        double costPerKm = 1.5;
        double cost = 0;
        for (Customer customer : discardList){
            cost += (customer.distance(customer.endPoint))*costPerKm;
        }
        return cost;
    }

    public List<SubGraph> swapBetweenRoutes(List<SubGraph> routes, int maxIterations, String objective, int insertionMethod){
        /*
         * Set number of iterations
         * At each iteration, pick 2 random routes
         * Swap a random passenger (pickup and drop-off) in one
         * route for a random passenger in the other route
         * Check for feasibility and for improvements
         * if improved: swap original routes for new routes
         * start again for new graph array at new iteration
         */

        int iterations = 0;
        List<SubGraph> bestOrder = routes;
        List<SubGraph> newOrder;
        Random r = new Random();
        int feasible = 0;
        int improvements = 0;

        while (iterations < maxIterations){
            // generate 4 random numbers:
            // 2 for selecting routes
            // 2 for selecting passengers
            int r1 = r.nextInt((bestOrder.size() - 1) + 1);
            int r2 = r.nextInt((bestOrder.size() - 1) + 1);
            while (r1 == r2){
                r2 = r.nextInt((bestOrder.size() - 1) + 1);
            }
            int r3 = r.nextInt((bestOrder.get(r1).getSize() - 2) + 1);
            int r4 = r.nextInt((bestOrder.get(r2).getSize() - 2) + 1);

            List<SubGraph> currentJourney = copyJourney(bestOrder);
            SubGraph firstRoute = currentJourney.get(r1);
            while (firstRoute.size == 0){
                r1 = r.nextInt((bestOrder.size() - 1) + 1);
                firstRoute = bestOrder.get(r1);
            }
            //SubGraph beforeRemoval1 = new SubGraph(1000, firstRoute);
            SubGraph beforeRemoval1 = SerializationUtils.clone(firstRoute);

            SubGraph secondRoute = currentJourney.get(r2);
            while (secondRoute.size == 0 || r2 == r1){
                r2 = r.nextInt((bestOrder.size() - 1) + 1);
                secondRoute = bestOrder.get(2);
            }
            //SubGraph beforeRemoval2 = new SubGraph(1001, secondRoute);
            SubGraph beforeRemoval2 = SerializationUtils.clone(secondRoute);

            //System.out.println("r3: "+r3+" r4: "+r4);
            Customer c1 = firstRoute.get(r3).customer;
            Customer c2 = secondRoute.get(r4).customer;
            int[] firstRoutePassengerLocations = getCustomerVertexes(firstRoute, r3);
            //System.out.printf("First route passenger locations: %d and %d\n", firstRoutePassengerLocations[0], firstRoutePassengerLocations[1]);
            //System.out.println("First route size: "+firstRoute.size);
            int[] secondRoutePassengerLocations = getCustomerVertexes(secondRoute, r4);
            //System.out.printf("Second route passenger locations: %d and %d\n", secondRoutePassengerLocations[0], secondRoutePassengerLocations[1]);
            //System.out.println("Second route size: "+secondRoute.size);


            //System.out.println("Removing passengers from each route...");
            //System.out.println(beforeRemoval1.printGraph());
            Vertex[] firstPairs = beforeRemoval1.removePassenger(firstRoutePassengerLocations[0], firstRoutePassengerLocations[1]);
            if ((firstPairs) == null) continue;
            Vertex[] secondPairs = beforeRemoval2.removePassenger(secondRoutePassengerLocations[0], secondRoutePassengerLocations[1]);
            if ((secondPairs) == null) continue;
            List<Customer> firstCustomerList = insertCustomer(secondPairs[0].customer, beforeRemoval1.route.succession, firstRoute.route.vehicle.capacity, insertionMethod);
            List<Customer> secondCustomerList = insertCustomer(firstPairs[0].customer, beforeRemoval2.route.succession, firstRoute.route.vehicle.capacity, insertionMethod);

            if (firstCustomerList == null || secondCustomerList == null) {
                iterations++;
                continue;
            } else {
                feasible++;
                firstRoute.route.succession = firstCustomerList;
                firstRoute.fillRoutes(firstRoute.route);
                secondRoute.route.succession = secondCustomerList;
                secondRoute.fillRoutes(secondRoute.route);

                if (checkForImprovement(bestOrder, currentJourney, objective)){
                    improvements ++;
                    bestOrder = currentJourney;
                }
            }
            iterations ++;
        }
        return bestOrder;
    }

    public  List<SubGraph> copyJourney(List<SubGraph> journey){
        List<SubGraph> newJourney = new ArrayList<>();
        for (SubGraph sg : journey){
            newJourney.add(SerializationUtils.clone(sg));
        }

        return  newJourney;
    }

    public List<SubGraph> oneToManySwap(List<SubGraph> routes, int maxIterations, String objective, int insertionMethod){
        int iterations = 0;
        double lowestCost = getTotalJourneyCost(routes);
        List<SubGraph> bestOrder = routes;
        List<SubGraph> currentOrder = routes;
        List<SubGraph> newOrder;
        Random r = new Random();
        int feasible = 0;
        int improvements = 0;

        while (iterations < maxIterations){
            currentOrder = copyJourney(bestOrder);
            Map<Integer, Integer> checkedCustomers = new HashMap<>();

            int r1 = r.nextInt((bestOrder.size() - 1) + 1);
            int r2 = r.nextInt((bestOrder.get(r1).getSize() - 2) + 1);

            //SubGraph beforeRemoval1 = new SubGraph(1000, firstRoute);
            for (SubGraph sg : bestOrder) {

                int pos = 0;
                Vertex current = sg.head;
                while (sg.getNextVertex(current) != null) {
                    //System.out.println("Checking vertex "+current.id);
                    try{
                        List<SubGraph> newJourney = copyJourney(currentOrder);
                        SubGraph firstRoute = newJourney.get(r1);
                        SubGraph secondRoute = null;
                        for (SubGraph route : newJourney){
                            if (route.id == sg.id){
                                secondRoute = route;
                            }
                        }
                        Customer c1 = firstRoute.get(r2).customer;
                        int[] firstRoutePassengerLocations = getCustomerVertexes(firstRoute, r2);
                        Vertex[] firstPairs = null;
                        while (firstPairs == null){
                            firstPairs = firstRoute.removePassenger(firstRoutePassengerLocations[0], firstRoutePassengerLocations[1]);
                        }
                        if (checkedCustomers.containsKey(current.customer.id)){
                            current = sg.getNextVertex(current);
                            continue;
                        } else{
                            checkedCustomers.put(current.customer.id, 1);
                        }
                        Customer c2 = current.customer;
                        int[] secondRoutePassengerLocations = getCustomerVertexes(secondRoute, pos);
                        Vertex[] secondPairs = secondRoute.removePassenger(secondRoutePassengerLocations[0], secondRoutePassengerLocations[1]);
                        if ((secondPairs) == null) continue;

                        List<Customer> firstCustomerList = insertCustomer(secondPairs[0].customer, firstRoute.route.succession, firstRoute.route.vehicle.capacity, insertionMethod);
                        List<Customer> secondCustomerList = insertCustomer(firstPairs[0].customer, secondRoute.route.succession, firstRoute.route.vehicle.capacity, insertionMethod);

                        if (firstCustomerList == null || secondCustomerList == null) {
                            iterations++;
                            continue;
                        } else {
                            feasible++;
                            firstRoute.route.succession = firstCustomerList;
                            firstRoute.fillRoutes(firstRoute.route);
                            sg.route.succession = secondCustomerList;
                            sg.fillRoutes(sg.route);

                            if (checkForImprovement(bestOrder, newJourney, objective)){
                                improvements ++;
                                bestOrder = newJourney;
                            }
                        }
                        iterations++;
                        current = sg.getNextVertex(current);
                        pos ++;
                    } catch (Exception e){
                        if (sg.getNextVertex(current) != null){
                            current = sg.getNextVertex(current);
                            pos ++;
                            continue;
                        } else{
                            break;
                        }
                    }
                    //System.out.println("finished checking vertex "+current.id);
                }
            }

            iterations ++;
        }


        return bestOrder;
    }

    public int[] getCustomerVertexes(SubGraph sg, int index){
        int pos1 = -1;
        int pos2 = -1;
        int customerId = sg.get(index).customer.id;
        Vertex current = sg.head;
        int count = 0;
        if (current.customer.id == customerId) pos1 = 0;
        while (sg.getNextVertex(current) != null){
            current = sg.getNextVertex(current);
            count ++;
            if (current.customer.id == customerId){
                if (pos1 == -1) {
                    pos1 = count;
                }else {
                    pos2 = count;
                }
            }
        }
        return new int[]{pos1, pos2};

    }

    public SubGraph twoOptSearchAlt(SubGraph sg){
        int maxIterations = 500;
        SubGraph newRoute;
        SubGraph bestRoute = sg;
        double bestScore = sg.getCost();
        double newScore;
        int swaps = 1;
        int improve = 0;
        int iterations = 0;
        long comparisons = 0;
        int feasibleCount = 0;

        while (swaps != 0){ //&& iterations < maxIterations) {
            swaps = 0;
            for (int i = 1; i < sg.getSize() - 2; i++) {
                for (int j = i+3; j < sg.getSize() - 3; j++) {
                    comparisons ++;
                    SubGraph testRoute = SerializationUtils.clone(bestRoute);
                    testRoute = swapEdges(testRoute, i, j);
                    Route r;
                    try{
                        r = testRoute.adjustRoute();
                    } catch (Exception e){
                        continue;
                    }

                    testRoute.fillRoutes(r);
                    double testScore = testRoute.getCost();
                    if (isFeasible(r)){
                        feasibleCount++;
                        if (testScore < bestScore) {
                            bestRoute = SerializationUtils.clone(testRoute);
                            bestScore = testScore;
                            swaps++;
                            improve++;
                        }
                    }
                }
            }
            iterations ++;
        }

        twoOptComparisons += comparisons;
        twoOptImprovements += improve;

        return bestRoute;
    }

    public SubGraph swapEdges(SubGraph sg, int i1, int i2){
        /*
         * Replace existing edge from v1 to v2 with new edge from
         * v3 to v4
         *
         * Input: i and j
         * create vertices: vi, vi+1, vj, vj+1
         * remove vi to vi+1, add vi to vj
         * remove vj to vj+1, add vi+1 to vj+1
         */
        Vertex v1 = sg.get(i1);
        //System.out.println("Vertex i1: "+v1.id);
        Vertex v2 = sg.get(i1+1);
        //System.out.println("Vertex i2: "+v2.id);
        Vertex v3 = sg.get(i2);
        //System.out.println("Vertex i3: "+v3.id);
        Vertex v4 = sg.get(i2+1);
        //System.out.println("Vertex i4: "+v4.id);

        Vertex prevVertexJ = sg.getPreviousVertex(v3);
        Vertex outgoingVertexIPlusOne = sg.getNextVertex(v2);

        // remove edge connecting i and i+1, keep i+1 outgoing edge in memory
        sg.removeEdge(v1.id, v2.id);
        // add edge from i to j
        sg.addEdge(v1, v3);
        // remove i+1 next vertex
        sg.removeEdge(v2.id, outgoingVertexIPlusOne.id);

        // remove edge connecting j and j+1
        sg.removeEdge(v3.id, v4.id);
        // add edge from i+1 to j+1
        sg.addEdge(v2, v4);
        // add edge from j to i+1 outgoing vertex
        sg.addEdge(v3, outgoingVertexIPlusOne);

        // remove edge from prev j vertex to j
        sg.removeEdge(prevVertexJ.id, v3.id);
        // add edge from j incoming vertex to i+1
        sg.addEdge(prevVertexJ, v2);

        //System.out.printf("Removing edge from Vertex %s to Vertex %s...\nAdding edge from Vertex %s to Vertex %s\n\n", v1.id, v2.id, v3.id, v4.id);

        return sg;
    }

    public double score(List<SubGraph> journey){
        /*
         * CURRENT FORMULA
         * (total distance travelled)*(avg travel efficiency)^2
         */
        double alpha = 0.1;
        double beta = 100;
        double cost = getTotalJourneyCost(journey);
        double satisfaction = getCustomerSatisfaction(journey);

        return alpha*cost - beta*satisfaction;
    }

    public int getTotalVehicleCapacity(){
        int capacity = 0;
        for (Vehicle vehicle : vehicles){
            capacity += vehicle.capacity;
        }
        return capacity;
    }

    public List<Customer> parseCustomers(String csvFile){
        List<Customer> customers = new ArrayList<>();
        String line = "";
        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] entry = line.split(cvsSplitBy);

                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
                try{
                    int id = Integer.parseInt(entry[0]);
                    int[] startPoint = {Integer.parseInt(entry[1]), Integer.parseInt(entry[2])};
                    int[] endPoint = {Integer.parseInt(entry[3]), Integer.parseInt(entry[4])};
                    Date startTime1 = formatter.parse(entry[5]);
                    Date endTime = formatter.parse(entry[6]);

                    Customer newCustomer = new Customer(id, startPoint, endPoint, startTime1, endTime);
                    customers.add(newCustomer);

                } catch (ParseException e){
                    e.printStackTrace();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return customers;
    }

    public void realTimeInsertion(String customerList, List<SubGraph> journey) throws ParseException {
        /*
         * Taking real-time customer orders to see if they can be accommodated
         * receive order,
         * add customer to discard pile,
         * run interRouteDiscardSwap for 5 minutes
         * new insert method: a parameter for time to insert
         * limitations: cannot change a customer due for collection in next 20 mins
         * to discard pile
         * create new state space
         */
        //SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a", Locale.ENGLISH);
        //Date converted = formatter.parse("29-12-2019 09:00:00 AM");
        Boolean added = false;
        DateTime startTime = new DateTime(2020, 03, 03, 9, 0, 0, 0);
        List<Customer> realTimeCustomers = parseCustomers(customerList);
        Random r = new Random();
        //int insertAt = r.nextInt(discardList.size()-1);
        //Collections.sort(realTimeCustomers);
        realTimeCustomers.sort((c1, c2) -> c1.timeWindow[0].compareTo(c2.timeWindow[0]));
        DateTime lastOrder = new DateTime(2020, 03, 03, 9, 0, 0, 0);
        List<SubGraph> updated = new ArrayList<>();
        for (int i=0; i<realTimeCustomers.size(); i++){
            DateTime orderTime = generateOrderTime(lastOrder, realTimeCustomers.get(i).timeWindow[0]);
            discardList.add(realTimeCustomers.get(i));
            /*
            discardList.add(realTimeCustomers.get(i+1));
            discardList.add(realTimeCustomers.get(i+2));
            discardList.add(realTimeCustomers.get(i+3));
            discardList.add(realTimeCustomers.get(i+4));
             */
            updated = realTimeInterRouteDiscardSwap(journey, 10, orderTime);
        }
        System.out.println("UPDATED JOURNEY");
        printJourney(updated);
    }

    public DateTime generateOrderTime(DateTime startTime, DateTime pickupRequest){
        //DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        //DateTime startTimeNew = dtf.parseDateTime(String.valueOf(startTime));
        //DateTime pickupRequestNew = dtf.parseDateTime(String.valueOf(pickupRequest));
        //long diff = Timestamp.valueOf(String.valueOf(pickupRequestNew)).getTime() - Timestamp.valueOf(String.valueOf(startTimeNew)).getTime() + 1;
        //long newTime = Timestamp.valueOf(String.valueOf(startTime)).getTime() + (long) (Math.random()*diff);
        Minutes diff = Minutes.minutesBetween(startTime, pickupRequest);
        Random r = new Random();
        int delay = r.nextInt((diff.getMinutes()+1));
        DateTime newTime = startTime.plusMinutes(delay);
        return newTime;
    }

    public List<SubGraph> realTimeInterRouteDiscardSwap(List<SubGraph> routes, int n, DateTime orderTime){
        /*
         * Remove n passengers from random vehicles and add to discard pile
         * Go through discard pile - for each passenger, loop through routes
         * (randomly) and try to insert passenger into route. If they cannot
         * be inserted, they can remain in the discard pile.
         */
        int maxIterations = 1;
        List<SubGraph> bestOrder = routes;
        int iterations = 0;
        int improvements = 0;
        double lowestCost = getTotalJourneyCost(routes);
        List<SubGraph> newOrder;
        Random rand = new Random();
        List<SubGraph> changed = new ArrayList<>();
        List<SubGraph> cannotRemoveMore = new ArrayList<>();
        try{
            while (iterations<maxIterations){
                List<SubGraph> testOrder = new ArrayList<>();
                for (SubGraph sg : bestOrder){
                    testOrder.add(SerializationUtils.clone(sg));
                }
                //remove n passengers from random graphs
                while (n>0 && cannotRemoveMore.size() < bestOrder.size()){
                    //randomly select route
                    int r1 = rand.nextInt((testOrder.size() - 1) + 1);
                    SubGraph currentRoute = testOrder.get(r1);
                    if (cannotRemoveMore.contains(currentRoute)) continue;
                    if (currentRoute.getSize() <= 2){
                        cannotRemoveMore.add(currentRoute);
                        continue;
                    }
                    //randomly select customer
                    int r2 = rand.nextInt((currentRoute.getSize() - 2) + 1);
                    try{
                        int[] firstRoutePassengerLocations = getCustomerVertexes(currentRoute, r2);
                        Vertex[] vertexPairs = currentRoute.removePassenger(firstRoutePassengerLocations[0], firstRoutePassengerLocations[1]);
                        if (vertexPairs == null || vertexPairs.length == 0){
                            n --;
                            continue;
                        }
                        discardList.add(vertexPairs[0].customer);
                        //System.out.printf("Customer %d added to discard pile\n", vertexPairs[0].customer.id);
                        n --;
                    } catch (OutOfMemoryError e){
                        n --;
                        continue;
                    }
                }

                //remove duplicates from discard pile
                List<Customer> alreadyPresent = new ArrayList<>();
                for (int i=0;i<discardList.size();i++){
                    if (alreadyPresent.contains(discardList.get(i))){
                        discardList.remove(i);
                        i --;
                    } else{
                        alreadyPresent.add(discardList.get(i));
                    }
                }

                List<SubGraph> finalList = new ArrayList<>();
                for (Iterator<Customer> iterator = discardList.iterator(); iterator.hasNext(); ) {
                    Customer currentCustomer = iterator.next();
                    int i = 0;
                    int r1 = rand.nextInt((testOrder.size() - 1) + 1);
                    while (i < testOrder.size()){
                        SubGraph sg = testOrder.get(r1);
                        List<Customer> currentCustomerList = insertRealTime(currentCustomer, sg, sg.route.vehicle.capacity, orderTime);
                        if (currentCustomerList != null){
                            //System.out.printf("Successfully added customer %d to route %s\n", currentCustomer.id, sg.id);
                            currentCustomerList = removeDuplicates(currentCustomerList);
                            testOrder.remove(sg);
                            //System.out.printf("Route %d size: %d", sg.id, currentCustomerList.size());
                            Route r = new Route(sg.route.id, currentCustomerList, sg.route.vehicle);
                            //sg.route.succession = currentCustomerList;
                            r.generateRoute();
                            //System.out.printf("Route %d size: %d", r.id, r.succession.size());
                            SubGraph sgNew = new SubGraph(sg.id);
                            sgNew.fillRoutes(r);
                            //System.out.printf("Route %d graph size: %d, list size: %d", sgNew.id, sgNew.getSize(), sgNew.route.succession.size());
                            testOrder.add(sgNew);

                        /*
                        sg.route.succession = currentCustomerList;
                        System.out.println(sg.route.succession.size());
                        sg.route.generateRoute();
                        System.out.println(sg.route.succession.size());
                        sg.fillRoutes(sg.route);
                        System.out.println(sg.route.succession.size());
                        System.out.println(sg.printGraph());
                         */

                            changed.add(sg);
                            //discardList.remove(currentCustomer);
                            iterator.remove();
                            //System.out.println("Discard list size: "+discardList.size());
                            break;
                        } else{
                            if (r1 == testOrder.size()-1){
                                r1 = 0;
                            } else {
                                r1 ++;
                            }
                        }
                        i ++;
                    }
                    interRouteSwapChanges = changed;
                }


/*
            //remove any duplicates from journey
            for (SubGraph sg : testOrder){
                System.out.printf("Checking graph %d for duplicates\n", sg.id);
                System.out.println(sg.printGraph());
                List<Customer> completed = new ArrayList<>();
                Vertex current = sg.head;
                while (sg.getNextVertex(current) != null){
                    current = sg.getNextVertex(current);
                    if (completed.contains(current.customer)){
                        System.out.println("REMOVING VERTEX "+current.id);
                        System.out.println("Type: "+current.type);
                        sg.removeVertex(current);
                    } else if (current.type == "dropoff"){
                        completed.add(current.customer);
                    }
                }



                Map<Vertex, Integer> completed = new HashMap<>();
                Vertex current = sg.head;
                completed.put(current, 1);
                while (sg.getNextVertex(current) != null){
                    current = sg.getNextVertex(current);
                    if (completed.containsKey(current)){
                        if (completed.get(current) == 2){
                            System.out.println("REMOVING VERTEX "+current.id);
                            sg.removeVertex(current);
                        } else{
                            int returnedValue = completed.get(current);
                            completed.replace(current, returnedValue+1);
                        }
                    } else{
                        completed.put(current, 1);
                    }
                }

                 */




                double cost = getTotalJourneyCost(testOrder);
                if (cost < lowestCost){
                    improvements ++;
                    lowestCost = cost;
                    bestOrder = testOrder;
                }
                iterations ++;
            }
            //System.out.println("Improvements made: " + improvements);
            interRouteImprovements = improvements;

            return bestOrder;
        } catch (NullPointerException e){
            return null;
        }

    }

    public List<Customer> insertRealTime(Customer customer, SubGraph currentRoute, int capacity, DateTime orderTime){
        List<Customer> currentPassengers = currentRoute.route.succession;
        int position = 1;
        int insertPosition = 1;
        Vertex current = currentRoute.head;
        DateTime newTime = current.customer.pickupTime;
        while (currentRoute.getNextVertex(current) != null){
            current = currentRoute.getNextVertex(current);
            if (current.type.equals("pickup")){
                newTime = current.customer.pickupTime;
            } else if (current.type.equals("dropoff")){
                newTime = current.customer.dropoffTime;
            }
            if (newTime.isAfter(orderTime)) {
                insertPosition = position;
                break;
            }
            position ++;
        }
        List<int[]> customerPositions = new ArrayList<>();
        if (currentPassengers.size() == 0){
            currentPassengers.add(customer);
            currentPassengers.add(customer);
            return currentPassengers;
        } else {
            for (int i = insertPosition; i < currentPassengers.size(); i++) {
                customerPositions.add(new int[]{i, i + 1});
                customerPositions.add(new int[]{i, i + 2});
                if (i == currentPassengers.size() - 1) customerPositions.add(new int[]{i + 1, i + 2});
            }
        }

        //System.out.println("Checking feasibility of possible positions for customer " + customer.id);
        //System.out.println("....................");
        //System.out.println("....................\n");
        //CONFIGURATION - REAL TIME
        int[] bestFeasibleRoute = getFirstFeasible(currentPassengers, customer, customerPositions, capacity);
        if (bestFeasibleRoute != null){
            //System.out.printf("First feasible indexes for customer %d: {%d, %d}\n", customer.id, firstFeasibleRoute[0], firstFeasibleRoute[1]);
            if (bestFeasibleRoute[0] > currentPassengers.size()){
                currentPassengers.add(customer);
            } else{
                currentPassengers.add(bestFeasibleRoute[0], customer);
            }
            if (bestFeasibleRoute[1] > currentPassengers.size()){
                currentPassengers.add(customer);
            } else{
                currentPassengers.add(bestFeasibleRoute[1], customer);
            }
            return currentPassengers;
        } else{
            // no feasible allocation, place passenger in queue with new priority
            return null;
        }
    }

    public static Color generateColor(){
        Random rand = new Random();
        // Java 'Color' class takes 3 floats, from 0 to 1.
        float r = rand.nextFloat();
        float g = rand.nextFloat();
        float b = rand.nextFloat();
        Color randomColor = new Color(r, g, b);
        return randomColor;
    }

    public List<Vehicle> populateFleet(int fleetSize, int capacity){
        List<Vehicle> newFleet = new ArrayList<>();
        for (int i=0;i<fleetSize;i++){
            newFleet.add(new Vehicle(i+1, new int[]{50, 50}, capacity));
        }
        return newFleet;
    }

    public double getCustomerSatisfaction(List<SubGraph> journey){
        double satisfaction = 0;
        int counter = 0;
        for (SubGraph sg : journey){
            satisfaction += sg.getCustomerSatisfaction();
            counter ++;
        }

        return (satisfaction/counter)+.5;
    }

    public void printJourney(List<SubGraph> journey){
        for (SubGraph graphRoute : journey) {
            System.out.println(graphRoute.printGraph());
            //System.out.println("Score: " + score(graphRoute));
            System.out.println("Cost: " + graphRoute.getCost());
            System.out.println();
        }

        System.out.println("Journey cost: " + getTotalJourneyCost(journey));
        System.out.println("\n\nNote: The following customers were not allocated to any vehicles ->");
        for (Customer customer : discardList){
            System.out.println("Customer " + customer.id);
        }
        System.out.println();
    }

    public static void main(String[] args){

    }
}

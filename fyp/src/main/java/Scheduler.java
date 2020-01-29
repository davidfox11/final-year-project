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

    public Scheduler(){
        discardList = new ArrayList<>();
    }

    public List<Vehicle> generateInitialSolution(){
        Comparator<Customer> priorityComparison = Comparator.comparing(Customer::getPriority);
        PriorityQueue<Customer> priorityQueue = new PriorityQueue<>( priorityComparison );
        List<Object> routes = new ArrayList<>();
        Boolean notFull = false;
        int maxAllocation;
        for (Customer customer : customers){
            customer.priority = customer.randomPriority(0, 100);
            priorityQueue.add(customer);
        }

        for (Vehicle vehicle : vehicles){
            vehicle.passengers = new ArrayList<>();
            int availableSeats = vehicle.capacity;
            while (availableSeats > 0){
                if (priorityQueue.isEmpty()){
                    break;
                }
                Customer newCustomer = priorityQueue.poll();
                vehicle.passengers.add(newCustomer);
                availableSeats --;
            }
        }

        for (Vehicle vehicle : vehicles){
            System.out.printf("**********\nVEHICLE %d\n**********\n", vehicle.id);
            for (Customer customer : vehicle.passengers){
                System.out.printf("** Customer %s\n", customer.id);
            }
            System.out.println("******************************\n\n");
        }

        return vehicles;
    }

    public List<Route> generateSolution(){
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
            System.out.println("Filling Vehicle " + vehicles.get(i).id);
            vehicles.get(i).passengers = new ArrayList<>();
            int availableSeats = vehicles.get(i).capacity;
            System.out.println(availableSeats);
            System.out.println(priorityQueue.size());
            /*
            while (availableSeats > 0){
                if (priorityQueue.isEmpty()){
                    System.out.println("BREAK");
                    break;
                }
                // inserting a new customer
                Customer nextCustomer = priorityQueue.poll();
                System.out.println("Priority Queue size: " + priorityQueue.size());
                List<Customer> newPassengerList = insertCustomer(nextCustomer, vehicles.get(i).passengers);
                if (newPassengerList != null){
                    vehicles.get(i).passengers = newPassengerList;
                    availableSeats --;
                    continue;
                } else if (newPassengerList == null && i<vehicles.size()-1){
                    System.out.printf("Customer %d not feasible. Placing back into priority queue with new priority %d\n", nextCustomer.id, nextCustomer.priority);
                    nextCustomer.priority = nextCustomer.randomPriority(0, 100);
                    if (!addAfterVehicleAssignment.contains(nextCustomer)){
                        addAfterVehicleAssignment.add(nextCustomer);
                    }
                    continue;
                } else {
                    System.out.printf("Customer %d cannot be assigned to any vehicle. Adding to discard pile\n", nextCustomer.id);
                    discardList.add(nextCustomer);
                }
            }
             */
            while (!priorityQueue.isEmpty()){
                Customer nextCustomer = priorityQueue.poll();
                List<Customer> newPassengerList = insertCustomer(nextCustomer, vehicles.get(i).passengers);
                if (newPassengerList != null){
                    vehicles.get(i).passengers = newPassengerList;
                    continue;
                } else if (newPassengerList == null && i<vehicles.size()-1){
                    System.out.printf("Customer %d not feasible. Placing back into priority queue with new priority %d\n", nextCustomer.id, nextCustomer.priority);
                    nextCustomer.priority = nextCustomer.randomPriority(0, 100);
                    if (!addAfterVehicleAssignment.contains(nextCustomer)){
                        addAfterVehicleAssignment.add(nextCustomer);
                    }
                    continue;
                } else{
                    System.out.printf("Customer %d cannot be assigned to any vehicle. Adding to discard pile\n", nextCustomer.id);
                    discardList.add(nextCustomer);
                }
            }
            Route route = new Route(vehicles.get(i).id, vehicles.get(i).passengers, vehicles.get(i));
            route.generateRoute();
            routes.add(route);
            for (Customer customer : addAfterVehicleAssignment){
                customer.assignmentAttempts ++;
                if (!priorityQueue.contains(customer)) priorityQueue.add(customer);
                continue;
            }
        }
        return routes;
    }

    public List<Customer> insertCustomer(Customer customer, List<Customer> currentPassengers){
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

        System.out.println("Checking feasibility of possible positions for customer " + customer.id);
        System.out.println("....................");
        System.out.println("....................\n");
        int[] firstFeasibleRoute = getFirstFeasible(currentPassengers, customer, customerPositions);
        if (firstFeasibleRoute != null){
            System.out.printf("First feasible indexes for customer %d: {%d, %d}\n", customer.id, firstFeasibleRoute[0], firstFeasibleRoute[1]);
            currentPassengers.add(firstFeasibleRoute[0], customer);
            currentPassengers.add(firstFeasibleRoute[1], customer);
            return currentPassengers;
        } else{
            // no feasible allocation, place passenger in queue with new priority
            return null;
        }
    }

    public int[] getFirstFeasible(List<Customer> passengers, Customer customer, List<int[]> customerPositions){
        for (int[] order : customerPositions){
            passengers.add(order[0], customer);
            passengers.add(order[1], customer);

            Route route = new Route(100, passengers, new Vehicle(100, new int[]{50,50}));
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
            if (route.getCurrentPassengers() > route.vehicle.capacity){
                return false;
            }
            if (route.getEndOfService().isAfter(route.vehicle.endTime)){
                return false;
            }
        }
        return true;
    }

    public SubGraph swap(SubGraph route, int i, int j){
        SubGraph newRoute = new SubGraph(route.id);
        int size = route.size;

        //for (int c=0;)
        /*
        //take array up to first point i and add to newTour
        int size = cities.size();
        for (int c = 0; c <= i - 1; c++) {
            newTour.add(cities.get(c));
        }

        //invert order between 2 passed points i and j and add to newTour
        int dec = 0;
        for (int c = i; c <= j; c++) {
            newTour.add(cities.get(j - dec));
            dec++;
        }

        //append array from point j to end to newTour
        for (int c = j + 1; c < size; c++) {
            newTour.add(cities.get(c));
        }
         */
        return route;
    }

    public void twoOptSearch(SubGraph sg){
        /*
         * Main local search method
         */
        SubGraph newRoute;
        double bestScore = score(sg);
        double newScore;
        int swaps = 1;
        int improve = 0;
        int iterations = 0;
        long comparisons = 0;

        while (swaps != 0) { //loop until no improvements are made.
            swaps = 0;

            Vertex currentVertex = sg.head;
            for (int i=1; i<sg.size-2; i++) {
                for (int j=i+1; j<sg.size-1; j++){
                    comparisons ++;
                    if ((sg.outgoingEdges.get(sg.get(i-1)).get(0).weight + sg.outgoingEdges.get(sg.get(j)).get(0).weight) >=
                            (sg.distance(sg.get(i), sg.get(j+1)) + sg.distance(sg.get(i-1), sg.get(j)))){

                        newRoute = swap(sg, i, j);
                    }
            }

            /*
            //initialise inner/outer loops avoiding adjacent calculations and making use of problem symmetry to half total comparisons.
            for (int i = 1; i < cities.size() - 2; i++) {
                for (int j = i + 1; j < cities.size() - 1; j++) {
                    comparisons++;
                    //check distance of line A,B + line C,D against A,C + B,D if there is improvement, call swap method.
                    if ((cities.get(i).distance(cities.get(i - 1)) + cities.get(j + 1).distance(cities.get(j))) >=
                            (cities.get(i).distance(cities.get(j + 1)) + cities.get(i - 1).distance(cities.get(j)))) {

                        newTour = swap(cities, i, j); //pass arraylist and 2 points to be swapped.

                        newDist = Length.routeLength(newTour);

                        if (newDist < bestDist) { //if the swap results in an improved distance, increment counters and update distance/tour
                            cities = newTour;
                            bestDist = newDist;
                            swaps++;
                            improve++;
                        }
                    }
                }

             */
            }
            iterations++;
        }



    }

    public double score(SubGraph sg){
        /*
         * CURRENT FORMULA
         * (total distance travelled)*(avg wait efficiency)*(avg travel efficiency)^2
         */
        double totalWaitTimeEfficiency = 0;
        double totalTravelEfficiency = 0;
        Vertex current = sg.head;
        Vertex nextVertex;
        double totalDistanceTravelled = current.customer.distance(current.vehicle.startPoint);
        while (sg.getNextVertex(current) != null){
            nextVertex = sg.getNextVertex(current);
            if (nextVertex.type == "pickup"){
                double waitTimeEfficiency = 0;
                double idealWaitTime = 0.1*(Minutes.minutesBetween(nextVertex.customer.timeWindow[0], nextVertex.customer.timeWindow[1]).getMinutes());
                double actualWaitTime = nextVertex.customer.waitTime;
                if (actualWaitTime <= idealWaitTime){
                    waitTimeEfficiency = 1;
                } else{
                    waitTimeEfficiency = (1-(actualWaitTime-idealWaitTime)/actualWaitTime);
                }
                totalWaitTimeEfficiency += waitTimeEfficiency;

            } else{
                double idealTravelDistance = nextVertex.customer.distance(nextVertex.customer.endPoint);
                double actualTravelDistance = sg.getActualTravelDistance(nextVertex);
                double travelEfficiency = (actualTravelDistance-idealTravelDistance)/actualTravelDistance;
                totalTravelEfficiency += travelEfficiency;
            }
            totalDistanceTravelled += sg.outgoingEdges.get(current).get(0).weight;
            current = nextVertex;
        }
        double averageTravelEfficiency = totalTravelEfficiency/sg.size;
        double averageWaitTimeEfficiency = totalWaitTimeEfficiency/sg.size;

        double score = (totalDistanceTravelled)*(averageWaitTimeEfficiency)*(Math.pow(averageTravelEfficiency, 2));
        return score;
    }

    public Route createRoute(int routeCount, Vehicle vehicle){
        /*
        * Takes list of vehicle assignments and creates new Route object
        */
        List<Customer> succession = new ArrayList<>();
        List<Customer> remainingPassengers = new ArrayList<>();
        for (Customer customer : vehicle.passengers){
            succession.add(customer);
            remainingPassengers.add(customer);
        }
        Random r = new Random();
        int low = 0;
        while (remainingPassengers.size() != 0){
            int high = succession.size()-1;
            int insertAtIndex = r.nextInt((high - low) + 1) + low;
            Customer nextCustomer = remainingPassengers.remove(r.nextInt((remainingPassengers.size() - 1) + 1));
            succession.add(insertAtIndex, nextCustomer);
        }
        Route route = new Route(routeCount, succession, vehicle);
        route.generateRoute();
        return route;
    }

    public int getTotalVehicleCapacity(){
        int capacity = 0;
        for (Vehicle vehicle : vehicles){
            capacity += vehicle.capacity;
        }
        return capacity;
    }

    public List<Customer> parseCustomers(){
        List<Customer> customers = new ArrayList<>();
        String csvFile = "customers.csv";
        String line = "";
        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] entry = line.split(cvsSplitBy);

                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a", Locale.ENGLISH);
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

    public static Color generateColor(){
        Random rand = new Random();
        // Java 'Color' class takes 3 floats, from 0 to 1.
        float r = rand.nextFloat();
        float g = rand.nextFloat();
        float b = rand.nextFloat();
        Color randomColor = new Color(r, g, b);
        return randomColor;
    }

    public List<Vehicle> populateFleet(int fleetSize){
        List<Vehicle> newFleet = new ArrayList<>();
        for (int i=0;i<fleetSize;i++){
            newFleet.add(new Vehicle(i+1, new int[]{50, 50}));
        }
        return newFleet;
    }
}

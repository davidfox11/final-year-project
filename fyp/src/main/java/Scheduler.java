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
    /*
        if (customers.size() < getTotalVehicleCapacity()){
            notFull = true;
            maxAllocation = (getTotalVehicleCapacity() / customers.size()) + 1;
        }
    */
        for (Vehicle vehicle : vehicles){
            vehicle.passengers = new ArrayList<>();
            int availableSeats = vehicle.capacity;
            while (availableSeats > 0){
                if (priorityQueue.isEmpty()){
                    System.out.println("Empty queue, leaving now");
                    break;
                }
                System.out.println(priorityQueue.size());
                Customer newCustomer = priorityQueue.poll();
                System.out.println(newCustomer.printCustomer());
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
            Customer nextCustomer = remainingPassengers.remove(r.nextInt((remainingPassengers.size()-1)+1)+0);
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
                    System.out.println("{"+newCustomer.startPoint[0]+","+newCustomer.startPoint[1]+"}");
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

    public static void main(String[] args){
        Scheduler scheduler = new Scheduler();
        scheduler.vehicles = scheduler.populateFleet(4);
        scheduler.customers = scheduler.parseCustomers();
        Plot myPlot = new Plot("Passenger Distribution",0,400,2,0,400,2);
        for (Customer customer : scheduler.customers){
            myPlot.setColor(generateColor());
            myPlot.addPoint(customer.startPoint[0], customer.startPoint[1]);
            myPlot.setConnected(true);
            myPlot.addPoint(customer.endPoint[0], customer.endPoint[1]);
            myPlot.setConnected(false);
            System.out.println(customer.printCustomer());
        }

        List<Vehicle> vehicles = scheduler.generateInitialSolution();

        List<Route> routes = new ArrayList<>();
        for (int i=0;i<vehicles.size();i++){
            routes.add(scheduler.createRoute(i+1, vehicles.get(i)));
        }

        for (int i=0;i<routes.size();i++){
            System.out.println(routes.get(i).printRoute());
        }




    }
}

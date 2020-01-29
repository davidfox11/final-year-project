import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class Route {
    int id;
    List<Customer> succession;
    Vehicle vehicle;
    DateTime beginningOfService;
    DateTime endOfService;
    List<Customer> collectedCustomers;

    public Route(int id, List<Customer> succession, Vehicle vehicle){
        this.id = id;
        this.succession = succession;
        this.vehicle = vehicle;
    }

    public int getCurrentPassengers(){
        List<Customer> pickedUp = new ArrayList<>();
        int total = 0;
        for (Customer customer : succession){
            if (pickedUp.contains(customer)){
                pickedUp.remove(customer);
                total --;
            } else{
                pickedUp.add(customer);
                total ++;
            }
        }
        return total;
    }

    public int getPositionOf(Customer customer){
         return succession.indexOf(customer);
    }


    public DateTime getBeginningOfService(){
        double distanceFromDepot = succession.get(0).distance(vehicle.startPoint);
        beginningOfService = succession.get(0).timeWindow[0].minus((long)distanceFromDepot);
        return beginningOfService;
    }

    public DateTime getEndOfService(){
        Customer lastCustomer = succession.get(succession.size()-1);
        double distanceToDepot = lastCustomer.distance(true, vehicle.startPoint);
        endOfService = lastCustomer.timeWindow[1].plus((long)distanceToDepot);
        return endOfService;
    }

    public void fillDepartureAndWaitTimes(){
        collectedCustomers = new ArrayList<>();
        for (int i=0;i<succession.size();i++){
            if (collectedCustomers.contains(succession.get(i))){
                fillArrivalAndTravelTimes(i);
                collectedCustomers.remove(succession.get(i));
            } else{
                collectedCustomers.add(succession.get(i));
                Customer currentCustomer = succession.get(i);
                if (i==0){
                    currentCustomer.pickupTime = currentCustomer.timeWindow[0];
                    currentCustomer.waitTime = 0;
                } else{
                    Customer previousCustomer = succession.get(i-1);
                    if (collectedCustomers.contains(previousCustomer)){  // if previous customer was picked up
                        double timeToCustomer = previousCustomer.distance(currentCustomer);
                        DateTime timeOfArrival = previousCustomer.pickupTime.plusMinutes((int)(timeToCustomer/vehicle.averageSpeed));
                        if (timeOfArrival.isBefore(currentCustomer.timeWindow[0]))
                        {
                            currentCustomer.pickupTime = currentCustomer.timeWindow[0];
                            currentCustomer.waitTime = 0;
                        } else{
                            currentCustomer.pickupTime = timeOfArrival;
                            currentCustomer.waitTime = Math.abs(Minutes.minutesBetween(timeOfArrival, currentCustomer.timeWindow[0]).getMinutes());
                        }
                    } else{ // if previous customer was dropped off
                        collectedCustomers.add(previousCustomer);
                        double timeToCustomer = previousCustomer.distance(true, currentCustomer);
                        DateTime timeOfArrival = previousCustomer.dropoffTime.plusMinutes((int)(timeToCustomer/vehicle.averageSpeed));
                        if (timeOfArrival.isBefore(currentCustomer.timeWindow[0]))
                        {
                            currentCustomer.pickupTime = currentCustomer.timeWindow[0];
                            currentCustomer.waitTime = 0;
                        } else{
                            currentCustomer.pickupTime = timeOfArrival;
                            currentCustomer.waitTime = Math.abs(Minutes.minutesBetween(timeOfArrival, currentCustomer.timeWindow[0]).getMinutes());
                        }
                    }
                }
            }
        }
    }

    public void fillArrivalAndTravelTimes(Integer i){
        Customer currentCustomer = succession.get(i);
        Customer previousCustomer = succession.get(i-1);
        if (collectedCustomers.contains(previousCustomer)){ // if previous customer was picked up
            double timeToCustomer = previousCustomer.distance(currentCustomer.endPoint);
            DateTime timeOfArrival = previousCustomer.pickupTime.plusMinutes((int)(timeToCustomer/vehicle.averageSpeed));
            if (timeOfArrival.isBefore(currentCustomer.timeWindow[1]))
            {
                currentCustomer.dropoffTime = currentCustomer.timeWindow[1];
            } else{
                currentCustomer.dropoffTime= timeOfArrival;
            }
            currentCustomer.travelTime = Math.abs(Minutes.minutesBetween(timeOfArrival, currentCustomer.pickupTime).getMinutes());
        } else { // if previous customer was dropped off
            collectedCustomers.add(previousCustomer);
            double timeToCustomer = previousCustomer.distance(true, currentCustomer.endPoint);
            DateTime timeOfArrival = previousCustomer.dropoffTime.plusMinutes((int)(timeToCustomer/vehicle.averageSpeed));
            if (timeOfArrival.isBefore(currentCustomer.timeWindow[1]))
            {
                currentCustomer.dropoffTime = currentCustomer.timeWindow[1];
            } else{
                currentCustomer.dropoffTime = timeOfArrival;
            }
            currentCustomer.travelTime = Math.abs(Minutes.minutesBetween(timeOfArrival, currentCustomer.pickupTime).getMinutes());
        }
    }

    public void generateRoute(){
        getBeginningOfService();
        getEndOfService();
        fillDepartureAndWaitTimes();
    }

    public String printRoute(){
        List<Integer> collectedCustomers = new ArrayList<>();
        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("HH:mm:ss");
        String message = String.format("*********************************** ROUTE %d ************************************\n", id);
        for (Customer customer : succession){
            if (collectedCustomers.contains(customer.id)){
                String dropoff = dtfOut.print(customer.dropoffTime);
                message += String.format("*\tDROPOFF: Customer %d at [%d, %d]. TIME: %s TRAVEL TIME: %d minutes\n", customer.id, customer.endPoint[0], customer.endPoint[1], dropoff, customer.travelTime);
            } else{
                String pickup = dtfOut.print(customer.pickupTime);
                message += String.format("*\tPICKUP: Customer %d at [%d, %d]. TIME: %s WAIT TIME: %d minutes\n", customer.id, customer.startPoint[0], customer.startPoint[1], pickup, customer.waitTime);
                collectedCustomers.add(customer.id);
            }
        }
        message += "***********************************************************************************\n\n";
        return message;
    }

}

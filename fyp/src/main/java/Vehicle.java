

/*
* Vehicle class for vehicle configuration
* The following parameters:
* id, capacity, startPoint
*/

import java.util.List;

public class Vehicle {
    int id;
    int capacity = 5;
    int[] startPoint;
    List<Customer> passengers;
    int averageSpeed = 50;

    public Vehicle(int id, int[] startPoint){
        this.id = id;
        this.startPoint = startPoint;
        //List<Customer> passengers = new ArrayList<>();
    }

}

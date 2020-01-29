

/*
* Vehicle class for vehicle configuration
* The following parameters:
* id, capacity, startPoint
*/

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

public class Vehicle {
    int id;
    int capacity = 5;
    int[] startPoint;
    List<Customer> passengers;
    int averageSpeed = 30;
    DateTime endTime;

    public Vehicle(int id, int[] startPoint){
        String string = "29/12/2019 13:00:00";
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        endTime = formatter.parseDateTime(string);
        this.id = id;
        this.startPoint = startPoint;
        //List<Customer> passengers = new ArrayList<>();
    }

}

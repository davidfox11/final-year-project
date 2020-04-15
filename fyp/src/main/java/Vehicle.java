

/*
* Vehicle class for vehicle configuration
* The following parameters:
* id, capacity, startPoint
*/

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Serializable;
import java.util.List;

public class Vehicle implements Serializable {
    int id;
    int capacity;
    int[] startPoint;
    List<Customer> passengers;
    int averageSpeed = 20;
    DateTime endTime;

    public Vehicle(int id, int[] startPoint, int capacity){
        String string = "29/12/2019 13:00:00";
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        endTime = formatter.parseDateTime(string);
        this.id = id;
        this.startPoint = startPoint;
        this.capacity = capacity;
        //List<Customer> passengers = new ArrayList<>();
    }

}

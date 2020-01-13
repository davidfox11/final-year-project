import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Customer {
    /*
    * Customer Class
    * Represent customers with the following parameters:
    * id, startPoint, endPoint, readyTime, endTime
    */
    int id;
    int[] startPoint;
    int[] endPoint;
    DateTime[] timeWindow;
    int priority = 0;
    DateTime pickupTime;
    DateTime dropoffTime;
    int waitTime;
    int travelTime;
    Boolean completed;

    public Customer(int id, int[] startPoint, int[] endPoint, Date timeWindow1, Date timeWindow2){
        this.id = id;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.timeWindow = new DateTime[]{new DateTime(timeWindow1), new DateTime(timeWindow2)};
    }

    public double distance(int[] target){
        //return euclidean distance from customer's startPoint to target
        return (Math.abs(startPoint[0]-target[0]))+Math.abs(startPoint[1]-target[1]);
        //return Math.sqrt(Math.pow(startPoint[0]-target[0], 2) + Math.pow(startPoint[1]-target[1], 2));
    }

    public double distance(Boolean isEndpoint, int[] target){
        //return euclidean distance from customer's endPoint to target
        if (isEndpoint){
            return (Math.abs(endPoint[0]-target[0]))+Math.abs(endPoint[1]-target[1]);
            //return Math.sqrt(Math.pow(endPoint[0]-target[0], 2) + Math.pow(endPoint[1]-target[1], 2));
        }
        return (Math.abs(startPoint[0]-target[0]))+Math.abs(startPoint[1]-target[1]);
        //return Math.sqrt(Math.pow(startPoint[0]-target[0], 2) + Math.pow(startPoint[1]-target[1], 2));
    }

    public double distance(Boolean isEndpoint, Customer target){
        //return euclidean distance from customer's endPoint to target
        if (isEndpoint){
            return (Math.abs(endPoint[0]-target.endPoint[0]))+Math.abs(endPoint[1]-target.endPoint[1]);
            //return Math.sqrt(Math.pow(endPoint[0]-target.endPoint[0], 2) + Math.pow(endPoint[1]-target.endPoint[1], 2));
        }
        return (Math.abs(startPoint[0]-target.startPoint[0]))+Math.abs(startPoint[1]-target.startPoint[1]);
    }

    public double distance(Customer target){
        //return euclidean distance from customer's startPoint to target
        return (Math.abs(startPoint[0]-target.startPoint[0]))+Math.abs(startPoint[1]-target.startPoint[1]);
        //return Math.sqrt(Math.pow(startPoint[0]-target.startPoint[0], 2) + Math.pow(startPoint[1]-target.startPoint[1], 2));
    }

    public int getPriority(){
        return priority;
    }

    public int randomPriority(int low, int high){
        Random r = new Random();
        return r.nextInt((high - low) + 1) + low;
    }

    public String printCustomer(){
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("HH:mm:ss");
        //String readyTimeString = dateFormat.format(timeWindow[0]); //2016/11/16 12:08:43
        String readyTimeString = dtfOut.print(timeWindow[0]);
        String endTimeString = dtfOut.print(timeWindow[1]);
        //String endTimeString = dateFormat.format(timeWindow[1]); //2016/11/16 12:08:43
        String pickup = String.format("[%d, %d]", startPoint[0], startPoint[1]);
        String dropoff = String.format("[%d, %d]", endPoint[0], endPoint[1]);
        return String.format("***Customer: %d***\nPickup: %s at %s\nDropoff: %s at %s\n\n", id, pickup, readyTimeString, dropoff, endTimeString);
    }

    public static void main(String[] args){
        System.out.println("Test");
    }
}

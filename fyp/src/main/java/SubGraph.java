import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubGraph{
    Vertex head;
    Vertex tail;
    Route route;
    int id;
    Map<String, Vertex> vertexMap;
    Map<Vertex, List<Edge>> outgoingEdges;
    Vertex firstVertex;
    int serviceTime;
    int size;

    public SubGraph(int id){
        this.id = id;
        outgoingEdges = new HashMap<>();
        vertexMap = new HashMap<>();
    }

    public Vertex getVertex(Customer customer, Boolean isPickup){
        return new Vertex(customer, isPickup);
    }

    public Vertex getHead(){
        return head;
    }

    public Vertex getTail(){
        return tail;
    }

    public Vertex addVertex(Customer customer, Boolean isPickup){
        Vertex v = new Vertex(customer, isPickup);
        if (isPickup){
            v.type = "pickup";
        } else{
            v.type = "dropoff";
        }
        vertexMap.put(v.id, v);
        outgoingEdges.putIfAbsent(v, new ArrayList<>());
        size ++;
        return v;
    }

    public int getActualTravelDistance(Vertex endVertex){
        String[] splitId = endVertex.id.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        Vertex currentVertex = vertexMap.get(splitId[0]);
        int totalDistance = 0;
        while (getNextVertex(currentVertex) != outgoingEdges.get(endVertex).get(0).toVertex){
            Vertex nextVertex = getNextVertex(currentVertex);
            totalDistance += outgoingEdges.get(currentVertex).get(0).weight;
        }
        return totalDistance;
    }

    public Vertex get(int index){
        int counter = 0;
        Vertex currentVertex = head;
        while (counter <= index){
            currentVertex = outgoingEdges.get(currentVertex).get(0).toVertex;
            counter ++;
        }
        return currentVertex;
    }

    public void removeVertex(Customer customer, Boolean isPickup){
        Vertex v = new Vertex(customer, isPickup);
        outgoingEdges.values().stream().forEach(e -> e.remove(v));
        outgoingEdges.remove(new Vertex(customer, isPickup));
    }

    public void addEdge(Vertex v1, Vertex v2){
        Edge e = new Edge(v1, v2, v1.getWeight(v2));
        outgoingEdges.get(v1).add(e);
    }

    public void addEdge(String id1, String id2){
        Vertex v1 = vertexMap.get(id1);
        Vertex v2 = vertexMap.get(id2);
        Edge e = new Edge(v1, v2, v1.getWeight(v2));
        outgoingEdges.get(v1).add(e);
    }

    public void removeEdge(Customer customer1, Boolean isPickup1, Customer customer2, Boolean isPickup2){
        Vertex v1 = new Vertex(customer1, isPickup1);
        Vertex v2 = new Vertex(customer2, isPickup2);
        List<Edge> outgoingEdge = outgoingEdges.get(v1);
        if (outgoingEdge != null) outgoingEdge.remove(v2);
    }

    public double distance(Vertex v1, Vertex v2){
        double distance = 0;
        Vertex currentVertex = v1;
        while (currentVertex != v2){
            distance += outgoingEdges.get(currentVertex).get(0).weight;
            currentVertex = outgoingEdges.get(currentVertex).get(0).toVertex;
        }
        return distance;
    }

    public Vertex getNextVertex(Vertex v){
        if (outgoingEdges.get(v).size() == 0){
            return null;
        } else {
            return outgoingEdges.get(v).get(0).toVertex;
        }
    }

    public int getNextVertexCost(Vertex v){
        return outgoingEdges.get(v).get(0).weight;
    }

    public int getTotalTime(int id){
        return Math.abs(Minutes.minutesBetween(route.getBeginningOfService(), route.getEndOfService()).getMinutes());
    }

    public void populateGraph(){

    }

    public void fillRoutes(Route route){
        serviceTime = Math.abs(Minutes.minutesBetween(route.getBeginningOfService(), route.getEndOfService()).getMinutes());
        Vertex previousVertex;
        Vertex currentVertex;
        List<Customer> collectedCustomers = new ArrayList<>();
        previousVertex = addVertex(route.succession.get(0), true);
        head = previousVertex;
        collectedCustomers.add(route.succession.get(0));
        for (int i=1;i<route.succession.size();i++){
            Customer currentCustomer = route.succession.get(i);
            Customer previousCustomer = route.succession.get(i-1);
            if (collectedCustomers.contains(currentCustomer)){
                currentVertex = addVertex(currentCustomer, false);
                collectedCustomers.remove(currentCustomer);
                if (collectedCustomers.contains(previousCustomer)){
                    addEdge(previousVertex, currentVertex);
                } else{
                    addEdge(previousVertex, currentVertex);
                }
            } else{
                currentVertex = addVertex(currentCustomer, true);
                collectedCustomers.add(currentCustomer);
                if (collectedCustomers.contains(previousCustomer)){
                    addEdge(previousVertex, currentVertex);
                } else{
                    addEdge(previousVertex, currentVertex);
                }
            }
            previousVertex = currentVertex;
            if (i == route.succession.size()-1) tail = currentVertex;
        }
    }

    public String printGraph(){
        Vertex current = head;
        Vertex nextVertex;
        List<Integer> collectedCustomers = new ArrayList<>();
        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("HH:mm:ss");
        String message = String.format("************************ GRAPH %d ************************\n", id);
        message += String.format("*\tPICKUP: Customer %d at [%d, %d]. TIME: %s WAIT TIME: %d minutes\n", current.customer.id, current.customer.startPoint[0], current.customer.startPoint[1], dtfOut.print(current.customer.timeWindow[0]), current.customer.waitTime);
        collectedCustomers.add(current.customer.id);
        while (getNextVertex(current) != null){
            nextVertex = getNextVertex(current);
            int cost = getNextVertexCost(current);
            if (collectedCustomers.contains(nextVertex.customer.id)){
                String dropoff = dtfOut.print(nextVertex.customer.dropoffTime);
                message += String.format("*\tDROPOFF: Customer %d at [%d, %d]. TIME: %s TRAVEL TIME: %d minutes\n", nextVertex.customer.id, nextVertex.customer.endPoint[0], nextVertex.customer.endPoint[1], dropoff, nextVertex.customer.travelTime);
            } else{
                String pickup = dtfOut.print(nextVertex.customer.pickupTime);
                message += String.format("*\tPICKUP: Customer %d at [%d, %d]. TIME: %s WAIT TIME: %d minutes\n", nextVertex.customer.id, nextVertex.customer.startPoint[0], nextVertex.customer.startPoint[1], pickup, nextVertex.customer.waitTime);
                collectedCustomers.add(nextVertex.customer.id);
            }
            current = nextVertex;
        }
        return message;
    }

    public static void main(String[] args){
        SubGraph sg = new SubGraph(1);
        Scheduler scheduler = new Scheduler();
        List<Customer> customers = scheduler.parseCustomers();
        Customer firstCustomer = customers.get(0);
        Vertex v1 = sg.addVertex(customers.get(0), true);
        Vertex v2 = sg.getVertex(firstCustomer, true);
        System.out.println(v1.id);
        System.out.println(v2.id);
        if (v1.equals(v2)){
            System.out.println("True");
        } else{
            System.out.println("False");
        }
    }

}

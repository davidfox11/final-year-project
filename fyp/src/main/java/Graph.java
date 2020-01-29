import org.joda.time.Minutes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    List<SubGraph> routes;
    Map<String, Vertex> vertexMap;
    Map<Vertex, List<Edge>> outgoingEdges;

    public Graph(List<Customer> customers, List<Vehicle> vehicles){
        vertexMap = new HashMap<>();
        outgoingEdges = new HashMap<>();
        routes = new ArrayList<>();
        for (Customer customer : customers){
            addVertex(customer, true);
            addVertex(customer, false);
        }
        addDepot(vehicles.get(0));
    }

    public Vertex addDepot(Vehicle vehicle){
        return new Vertex(vehicle);
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
        return v;
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

    public void addEdge(Customer customer1, Boolean isPickup1, Customer customer2, Boolean isPickup2){
        Vertex v1 = vertexMap.get(getVertexId(customer1, isPickup1));
        Vertex v2 = vertexMap.get(getVertexId(customer2, isPickup2));
        Edge e = new Edge(v1, v2, v1.getWeight(v2));
        outgoingEdges.get(v1).add(e);
    }

    public String getVertexId(Customer customer, Boolean isPickup){
        String msg = Integer.toString(customer.id);
        if (isPickup){
            msg += "a";
        } else {
            msg += "b";
        }
        return msg;
    }

    public SubGraph createSubGraph(Route route){
        SubGraph sg = new SubGraph(route.id);
        Vertex previousVertex;
        Vertex currentVertex;
        sg.serviceTime = Math.abs(Minutes.minutesBetween(route.getBeginningOfService(), route.getEndOfService()).getMinutes());
        List<Customer> collectedCustomers = new ArrayList<>();
        previousVertex = vertexMap.get(getVertexId(route.succession.get(0), true));
        sg.addVertex(route.succession.get(0), true);
        sg.head = previousVertex;
        collectedCustomers.add(route.succession.get(0));
        for (int i=1;i<route.succession.size();i++){
            Customer currentCustomer = route.succession.get(i);
            Customer previousCustomer = route.succession.get(i-1);
            if (collectedCustomers.contains(currentCustomer)){
                currentVertex = vertexMap.get(getVertexId(currentCustomer, false));
                sg.addVertex(currentCustomer, false);
                collectedCustomers.remove(currentCustomer);
            } else{
                currentVertex = vertexMap.get(getVertexId(currentCustomer, true));
                sg.addVertex(currentCustomer, true);
                collectedCustomers.add(currentCustomer);
            }
            if (collectedCustomers.contains(previousCustomer)){
                previousVertex = vertexMap.get(getVertexId(previousCustomer, true));
                addEdge(previousVertex, currentVertex);
                sg.addEdge(previousVertex.id, currentVertex.id);
            } else{
                previousVertex = vertexMap.get(getVertexId(previousCustomer, false));
                addEdge(previousVertex, currentVertex);
                sg.addEdge(previousVertex.id, currentVertex.id);
            }
            if (i == route.succession.size()-1) sg.tail = currentVertex;
        }
        return sg;
    }

    public void removeEdge(Customer customer1, Boolean isPickup1, Customer customer2, Boolean isPickup2){
        Vertex v1 = new Vertex(customer1, isPickup1);
        Vertex v2 = new Vertex(customer2, isPickup2);
        List<Edge> outgoingEdge = outgoingEdges.get(v1);
        if (outgoingEdge != null) outgoingEdge.remove(v2);
    }
}

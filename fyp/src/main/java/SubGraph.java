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
        // get the actual distance travelled from one customer's pickup point to their drop-off point
        String[] splitId = endVertex.id.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        String pickupVertex = splitId[0]+"a";
        Vertex currentVertex = vertexMap.get(pickupVertex);
        int totalDistance = 0;
        while (currentVertex != endVertex){
            Vertex nextVertex = getNextVertex(currentVertex);
            //System.out.printf("Current: %s, End: %s\n", currentVertex.id, endVertex.id);
            //System.out.println(outgoingEdges.get(currentVertex).get(0).weight);
            //System.out.println(currentVertex.id);
            //System.out.println(outgoingEdges.get(currentVertex));
            if (outgoingEdges.get(currentVertex).size() == 0) break;
            totalDistance += outgoingEdges.get(currentVertex).get(0).weight;
            currentVertex = nextVertex;
        }
        return totalDistance*50;
    }

    public int getSize(){
        size = 1;
        Vertex current = head;
        while (getNextVertex(current) != null){
            current = getNextVertex(current);
            size ++;
        }
        return size;
    }

    public Vertex get(int index){
        int counter = 0;
        Vertex currentVertex = head;
        while (counter < index){
            currentVertex = getNextVertex(currentVertex);
            counter ++;
        }
        return currentVertex;
    }

    public Vertex getPreviousVertex(Vertex v){
        Vertex currentVertex = head;
        if (outgoingEdges.get(currentVertex).get(0).toVertex.id.equals(v.id)) return currentVertex;
        while (getNextVertex(currentVertex) != null){
            currentVertex = getNextVertex(currentVertex);
            System.out.println(currentVertex.id);
            if (outgoingEdges.get(currentVertex).get(0) == null) return null;
            if (outgoingEdges.get(currentVertex).get(0).toVertex.id.equals(v.id)) return currentVertex;
        }
        return null;
    }

    public void removeVertex(Customer customer, Boolean isPickup){
        Vertex v = new Vertex(customer, isPickup);
        outgoingEdges.values().stream().forEach(e -> e.remove(v));
        outgoingEdges.remove(new Vertex(customer, isPickup));
    }

    public Edge addEdge(Vertex v1, Vertex v2){
        Edge e = new Edge(v1, v2, v1.getWeight(v2));
        //System.out.printf("Edge cost: %d\n", v1.getWeight(v2));
        outgoingEdges.get(v1).add(e);
        //System.out.println(outgoingEdges.get(v1).get(0).weight);
        return e;
    }

    public Edge addEdge(String id1, String id2){
        Vertex v1 = vertexMap.get(id1);
        Vertex v2 = vertexMap.get(id2);
        Edge e = new Edge(v1, v2, v1.getWeight(v2));
        outgoingEdges.get(v1).add(e);
        return e;
    }

    public Edge removeEdge(String id1, String id2){
        Vertex v1 = vertexMap.get(id1);
        Vertex v2 = vertexMap.get(id2);
        List<Edge> outgoingEdge = outgoingEdges.get(v1);
        if (outgoingEdge != null){
            Edge e = outgoingEdge.get(0);
            outgoingEdge.remove(0);
            return e;
        }
        return null;
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

    public void fillRoutes(Route newRoute){
        route = newRoute;
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
                    Edge e = addEdge(previousVertex, currentVertex);
                    //System.out.printf("Added edge: Cost %d", e.weight);
                } else{
                    Edge e = addEdge(previousVertex, currentVertex);
                    //System.out.printf("Added edge: Cost %d", e.weight);
                }
            } else{
                currentVertex = addVertex(currentCustomer, true);
                collectedCustomers.add(currentCustomer);
                if (collectedCustomers.contains(previousCustomer)){
                    Edge e = addEdge(previousVertex, currentVertex);
                    //System.out.printf("Added edge: Cost %d", e.weight);
                } else{
                    Edge e = addEdge(previousVertex, currentVertex);
                    //System.out.printf("Added edge: Cost %d", e.weight);
                }
            }
            previousVertex = currentVertex;
            if (i == route.succession.size()-1) tail = currentVertex;
        }
    }

    public Route adjustRoute(){
        List<Customer> newPassengerList = new ArrayList<>();
        //loop through sub-graph and add each item into newPassengerList in new order
        Vertex current = head;
        newPassengerList.add(current.customer);
        while (getNextVertex(current) != null){
            current = getNextVertex(current);
            //System.out.printf("Adding customer %d\n", current.customer.id);
            newPassengerList.add(current.customer);
        }
        //System.out.println(route.id);
        route.succession = newPassengerList;
        route.generateRoute();
        return route;
    }

    public Vertex[] removePassenger(int i, int j){
        /*
         * remove a passenger from the list and return the 2 vertices
         * i is the passengers pickup index
         * j is the passengers drop-off index
         */
        Boolean startVertex = false;
        Boolean endVertex = false;

        Vertex jVertex = get(j);
        Vertex previousVertexJ = getPreviousVertex(jVertex);
        if (previousVertexJ == null){
            startVertex = true;
        }
        Vertex nextVertexJ = getNextVertex(jVertex);
        if (nextVertexJ == null){
            endVertex = true;
        } else{
            Edge eJ = removeEdge(jVertex.id, nextVertexJ.id);
        }
        if (!startVertex) {
            removeEdge(previousVertexJ.id, jVertex.id);
            if (!endVertex) addEdge(previousVertexJ, nextVertexJ);
        }

        startVertex = false;
        endVertex = false;
        Vertex iVertex = get(i);
        Vertex previousVertexI = getPreviousVertex(iVertex);
        if (previousVertexI == null){
            startVertex = true;
        }
        Vertex nextVertexI = getNextVertex(iVertex);
        if (nextVertexI == null){
            endVertex = true;
        } else {
            Edge eI = removeEdge(iVertex.id, nextVertexI.id);
        }
        if (!startVertex) {
            removeEdge(previousVertexI.id, iVertex.id);
            if (!endVertex) addEdge(previousVertexI, nextVertexI);
        }

        System.out.printf("Customer %d has been removed from route %d\n", iVertex.customer.id, route.id);
        return new Vertex[]{iVertex, jVertex};
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
/*
        for (List<Edge> value : outgoingEdges.values()){
            System.out.printf("Vertex %s to Vertex %s: %d\n", value.get(0).fromVertex.id, value.get(0).toVertex.id, value.get(0).weight);
        }

 */
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

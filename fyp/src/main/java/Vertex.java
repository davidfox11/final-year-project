public class Vertex {
    String id;
    Customer customer;
    Boolean isPickup;
    int[] location;
    Vehicle vehicle;
    Edge incoming;
    Edge outgoing;
    String type;

    public Vertex(Customer customer, Boolean isPickup){
        this.id = Integer.toString(customer.id);
        this.customer = customer;
        if (isPickup){
            this.location = customer.startPoint;
            this.id += "a";
        } else{
            this.location = customer.endPoint;
            this.id += "b";
        }
    }

    public Vertex(Vehicle vehicle){
        this.id = Integer.toString(vehicle.id);
        this.vehicle = vehicle;
    }

    public int getWeight(Vertex vertex){
        return Math.abs(Math.abs(location[0]-vertex.location[0]) + Math.abs(location[1]-vertex.location[1]))/50;
    }

    public Boolean equals(Vertex v){
        return (v.id.equals(id));
    }
}

public class Vertex {
    Customer customer;
    Boolean isPickup;
    int[] location;
    Edge incoming;
    Edge outgoing;

    public Vertex(Customer customer, Boolean isPickup, int[] location){
        this.customer = customer;
        this.isPickup = isPickup;
        this.location = location;
    }
}

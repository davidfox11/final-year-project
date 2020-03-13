import java.io.Serializable;

public class Edge implements Serializable {
    Vertex fromVertex;
    Vertex toVertex;
    int weight;

    public Edge(Vertex fromVertex, Vertex toVertex, int weight){
        this.fromVertex = fromVertex;
        this.toVertex = toVertex;
        this.weight = weight;
    }
}

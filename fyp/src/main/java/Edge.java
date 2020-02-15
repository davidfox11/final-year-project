public class Edge {
    Vertex fromVertex;
    Vertex toVertex;
    int weight;

    public Edge(Vertex fromVertex, Vertex toVertex, int weight){
        this.fromVertex = fromVertex;
        this.toVertex = toVertex;
        this.weight = weight;
    }
}

import java.util.List;

public class Main {
    public static void main(String[] args){
        int a = 5;
        int b = a;
        b += 7;
        System.out.println(a);
        Scheduler scheduler = new Scheduler();
        scheduler.vehicles = scheduler.populateFleet(5);
        scheduler.customers = scheduler.parseCustomers();


        /*
        List<Vehicle> vehicles = scheduler.generateInitialSolution();


        List<Route> routes = new ArrayList<>();
        for (int i=0;i<vehicles.size();i++){
            routes.add(scheduler.createRoute(i+1, vehicles.get(i)));
        }
         */

        List<Route> routes = scheduler.generateSolution();

        Graph graph = new Graph(scheduler.customers, scheduler.vehicles);
        for (int i=0;i<routes.size();i++){
            //System.out.println(routes.get(i).printRoute());
            SubGraph sg = graph.createSubGraph(routes.get(i));
            sg.fillRoutes(routes.get(i));
            graph.routes.add(sg);
        }

/*
        SubGraph testGraph = graph.routes.get(0);
        System.out.println(testGraph.printGraph());
        scheduler.swapEdges(testGraph, 2, 6);
        Route r = testGraph.adjustRoute();
        testGraph.fillRoutes(r);
        System.out.println(testGraph.printGraph());

 */
/*

        SubGraph testGraph = graph.routes.get(0);
        System.out.println(testGraph.printGraph());
        System.out.println(testGraph.getSize());
        System.out.println("Score: " + scheduler.score(testGraph));


        SubGraph newTestGraph = scheduler.twoOptSearchAlt(testGraph);
        Route r = newTestGraph.adjustRoute();
        newTestGraph.fillRoutes(r);
        System.out.println("\n\nUpdated graph:");
        System.out.println(newTestGraph.printGraph());

        System.out.println("Score: " + scheduler.score(newTestGraph));

 */



        System.out.println();
        for (SubGraph graphRoute : graph.routes) {
            System.out.println(graphRoute.printGraph());
            System.out.println("Score: " + scheduler.score(graphRoute));
            System.out.println();
            System.out.println("Checking for null edges");

        }
        System.out.println("Average Score: "+scheduler.getAverageRouteScore(graph.routes));
        System.out.println("\n\nNote: The following customers were not allocated to any vehicles ->");
        for (Customer customer : scheduler.discardList){
            System.out.println("Customer " + customer.id);
        }

        List<SubGraph> newGraphRoutes = scheduler.swapBetweenRoutes(graph.routes);
        System.out.println("UPDATED GRAPHS");
        for (SubGraph graphRoute : newGraphRoutes) {
            System.out.println(graphRoute.printGraph());
            System.out.println("Score: " + scheduler.score(graphRoute));
            System.out.println();
        }
        System.out.println("Average Score: "+scheduler.getAverageRouteScore(graph.routes));




/*
        Plot myPlot = new Plot("Passenger Distribution",0,400,2,0,400,2);
        for (Customer customer : scheduler.customers){
            myPlot.setColor(scheduler.generateColor());
            myPlot.addPoint(customer.startPoint[0], customer.startPoint[1]);
            myPlot.setConnected(true);
            myPlot.addPoint(customer.endPoint[0], customer.endPoint[1]);
            myPlot.setConnected(false);
        }
*/



    }
}

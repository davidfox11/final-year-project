import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws ParseException, IOException {
        Scheduler scheduler = new Scheduler();
        GenerateCustomers generator = new GenerateCustomers("customers1.csv", 100);
        generator.generate();
        scheduler.vehicles = scheduler.populateFleet(5);
        scheduler.customers = scheduler.parseCustomers("customers1.csv");


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
            if (sg.outgoingEdges.get(sg.get(sg.getSize()-2)).get(0) == null) System.out.println("It's a null");
        }

/*
        // FOR TESTING TWO-OPT SEARCH

        SubGraph testGraph = graph.routes.get(0);
        System.out.println(testGraph.printGraph());
        System.out.println(testGraph.getSize());
        System.out.println("Score: " + scheduler.score(testGraph));
        System.out.println("Cost: " + testGraph.getCost());



        SubGraph newTestGraph = scheduler.twoOptSearchAlt(testGraph);
        Route r = newTestGraph.adjustRoute();
        newTestGraph.fillRoutes(r);
        System.out.println("\n\nUpdated graph:");
        System.out.println(newTestGraph.printGraph());

        System.out.println("Score: " + scheduler.score(newTestGraph));
        System.out.println("Cost: " + newTestGraph.getCost());
*/


        scheduler.printJourney(graph.routes);

/*
        List<SubGraph> updatedRoutes = new ArrayList<>();
        System.out.println("Initializing two-opt search...");
        for (SubGraph sg : graph.routes){
            System.out.printf("Optimising route number %d...\n\n", sg.id);
            SubGraph newGraph = scheduler.twoOptSearchAlt(sg);
            Route r = newGraph.adjustRoute();
            newGraph.fillRoutes(r);
            updatedRoutes.add(newGraph);
        }

        System.out.println("UPDATED ROUTES");
        for (SubGraph sg : updatedRoutes){
            System.out.println(sg.printGraph());
            System.out.println("Score: " + scheduler.score(sg));
            System.out.println("Cost: " + sg.getCost());
            System.out.println();
        }

        System.out.println("Journey cost: " + scheduler.getTotalJourneyCost(updatedRoutes));

        System.out.println("\n\nNote: The following customers were not allocated to any vehicles ->");
        for (Customer customer : scheduler.discardList){
            System.out.println("Customer " + customer.id);
        }
 */


        //List<SubGraph> newGraphRoutes = scheduler.swapBetweenRoutes(graph.routes);
        //List<SubGraph> newGraphRoutes = scheduler.interRouteDiscardSwap(graph.routes, 5);

        //scheduler.realTimeInsertion(graph.routes);



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

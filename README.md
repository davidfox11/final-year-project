# An Iterated Local Search algorithm for solving static and real-time instances of the Dial-a-Ride problem



This was completed as part of a final year project for a BSc in Computer Science from University College Cork.

The algorithm has been designed to service different types of customer order sets. There are 48 customer sets currently in this module, and the CustomerGenerator class can generate new customer sets.

To begin, you should set up an instance of the IteratedLocalSearch class. The following parameters are required:
  - **maxRunTime**: The maximum time the algorithm should run for
  - **resultsFile**: The file where you would like results to be stored
  -  **customerCount**: How many customer order sets to generate
  - **fleetSize**: the number of vehicles to dispatch
  - **vehicleCapacity**: The number of seats in each vehicle
  - **maxTimeWindow**: Maximum time between customer's pickup & drop-off time
  - **sparsity**: The maximum time between all pickup and drop-off time requests

```java
IteratedLocalSearch ils = new IteratedLocalSearch(1, 0, "customers2.csv", "../tests/test1.txt", 40, 5, 6,3, 2);
```

You can set whether the algorithm will solve for static instance of the DARP
or real-time instances. By default, it will solve for static instances. To
change this, type:
```java
ils.realTime = true;
```

There different methods for passenger allocation and for swapping passengers between routes. In the following lines you can configure different aspects of the algorithm.
Each heuristic is number coded with their parameter as shown:
1. first feasible insertion
2. best feasible insertion
3. one-to-one swap (maximum iterations)
4. one-to-many swap (maximum iterations)
5. large discard swap (removal constant)

```java
ils.insertionMethod = 2;
ils.swapMethod = new int[]{4, 100};
```

If you would like to print the exact vehicle routes to the terminal, set printRoutes = true. By default, this setting is false:
```java
ils.printRoutes = true;
```

Using JComponent, a grid is created which maps out the state space of all passenger pickup and drop-off requested locations. This is shown as soon as the algorithm begins creating solutions. When each route is finished, the algorithm will output on the grid the route that each vehicle has taken. By default, these grids are not shown. To show them, type the following:
```java
ils.jComponent = true;
```

# Setting Objectives
Depending on your preference, the algorithm will aim to optimise a particular metric. You can choose between:
  - minimising cost (keyword: cost),
  - maximising satisfaction (keyword: satisfaction),
  - minimising score (a weighted combination of cost and satisfaction, keyword: score).
```java
ils.objective = "cost";
```

By default, the algorithm will create solution from the customer sets already made.
However, if you would like to generate new customer sets, you just need to write the
following:
```java
ils.generateNewOrders = true;
```
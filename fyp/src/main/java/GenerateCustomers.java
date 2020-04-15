import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateCustomers {
    String filename;
    int customerCount;
    int maxTimeWindow;
    int sparsity;
    List<String[]> customers;

    public GenerateCustomers(String filename, int customerCount, int maxTimeWindow, int sparsity){
        this.filename = filename;
        this.customerCount = customerCount;
        this.maxTimeWindow = maxTimeWindow;
        this.sparsity = sparsity;
        customers = new ArrayList<>();
    }

    public int generateCustomerId(int id){
        return id;
    }

    public int[] generatePickupAndDropoff(){
        // generate starting coordinates and dropoff coordinates
        // total distance between coordinates should be between
        // 50 and 400
        int p1 = 0;
        int p2 = 0;
        int p3 = 0;
        int p4 = 0;

        Random r = new Random();

        while (Math.abs(p1 - p3) + Math.abs(p2-p4) < 50 || Math.abs(p1 - p3) + Math.abs(p2-p4) > 400){
            p1 = r.nextInt(400-10 + 1) + 10;
            p2 = r.nextInt(400-10 + 1) + 10;
            p3 = r.nextInt(400-10 + 1) + 10;
            p4 = r.nextInt(400-10 + 1) + 10;
        }

        return new int[]{p1, p2, p3, p4};
    }

    public String[] generateTimeWindow(){
        // Time window should be between 1hr and 3 hrs
        int startTimeWindow = 9;
        int endTimeWindow = 9 + maxTimeWindow;
        int endTime = endTimeWindow + 2;
        String date = "03-03-2020";
        Random r = new Random();
        int p1=0;
        int p2=0;
        int p3=0;
        int p4=0;
        String p1String;
        String p2String;
        String p3String;
        String p4String;
        double difference = 0;
        Boolean feasible = false;
        while (difference < 1 || difference > maxTimeWindow || !feasible) {
            feasible = false;
            p1 = r.nextInt((startTimeWindow + sparsity) - startTimeWindow + 1) + startTimeWindow;
            p2 = r.nextInt(60 + 1);
            p3 = r.nextInt((endTimeWindow) - (endTimeWindow-sparsity) + 1) + (endTimeWindow-sparsity) + 1;
            p4 = r.nextInt(60 + 1);
            /*
            p1 = r.nextInt(14 - 9 + 1) + 9;
            p2 = r.nextInt(60 + 1);
            p3 = r.nextInt(17 - 10 + 1) + 10;
            p4 = r.nextInt(60 + 1);
             */
            double minuteDifference = Math.abs(p4 - p3);
            minuteDifference = (minuteDifference) / 60;
            difference = (p2 - p1) + minuteDifference;
            if (p1 < p3){
                feasible = true;
            } else if (p1 == p3){
                if (p2 < p4){
                    feasible = true;
                }
            }
        }
        if (p1<10){
            p1String = "0"+Integer.toString(p1);
        } else{
            p1String = Integer.toString(p1);
        }
        if (p3<10){
            p3String = "0"+Integer.toString(p3);
        } else{
            p3String = Integer.toString(p3);
        }
        if (p2<10){
            p2String = "0"+Integer.toString(p2);
        } else{
            p2String = Integer.toString(p2);
        }
        if (p4<10){
            p4String = "0"+Integer.toString(p4);
        } else{
            p4String = Integer.toString(p4);
        }
        String pickupTime = String.format("03-03-2020 %s:%s:00", p1String, p2String);
        String dropoffTime = String.format("03-03-2020 %s:%s:00", p3String, p4String);

        return new String[]{pickupTime, dropoffTime};
    }

    public void generate() throws IOException {
        int counter = 0;
        while (counter < customerCount){
            int id = counter+1;
            int[] coordinates = generatePickupAndDropoff();
            String[] timeWindow = generateTimeWindow();
            String[] newCustomer = new String[]{Integer.toString(id), Integer.toString(coordinates[0]), Integer.toString(coordinates[1]),
                    Integer.toString(coordinates[2]), Integer.toString(coordinates[3]), timeWindow[0], timeWindow[1]};

            customers.add(newCustomer);
            counter ++;
        }

        createFile();

    }

    public String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    public String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public void createFile() throws IOException {
        File csvOutputFile = new File(filename);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            customers.stream()
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        }
    }
}

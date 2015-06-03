import java.util.ArrayList;
import java.util.HashMap;

public class Range implements Comparable<Range> {
    //starting time string
    public String startTime;

    //public String endTime;

    //integer start time
    public int startTimeSeconds;

    //public int endTimeSeconds;

    //the words said during this TimeRegion
    public String contents;

    //the localTF values
    public HashMap<String, Double> localTF = new HashMap<>();

    public double[] tfIdfVector;
    public double importance = 0;
    public double groupImportance = 0;
    int indexLocation;

    public Range(String startTime, String contents, int counter) {
        this.startTime = startTime;
        String[] timeArray = this.startTime.split(":");
        startTimeSeconds = Integer.valueOf(timeArray[0]) * 60 + Integer.valueOf(timeArray[1]);

//        this.endTime = endTime;
//        timeArray = this.endTime.split(":");
//        this.endTimeSeconds = Integer.valueOf(timeArray[0]) * 60 + Integer.valueOf(timeArray[1]);


        this.contents = contents;
        indexLocation = counter;
    }

    public int compareTo(Range o) {
        return Double.compare(this.groupImportance, o.groupImportance);
    }
}
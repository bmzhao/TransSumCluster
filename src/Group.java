import java.util.ArrayList;

/**
 * Brian Zhao && Victor Kwak
 * 5/3/15
 */
public class Group implements Comparable<Group> {
    private ArrayList<Range> group = new ArrayList<>();
    private double totalImportance = 0;

    public void add(Range range) {
        totalImportance += range.importance;
        group.add(range);
    }

    public Range get(int i) {
        return group.get(i);
    }

    public int size() {
        return group.size();
    }

    //sort will sort from greatest to least
    @Override
    public int compareTo(Group o) {
        return Double.compare((o.totalImportance / o.group.size()) , (this.totalImportance / this.group.size()));
    }

    public int groupLength() {
        String[] startTime = group.get(0).startTime.split(":");
        String[] endTime = group.get(group.size() - 1).startTime.split(":");
        return Integer.parseInt(endTime[0]) - Integer.parseInt(startTime[0]);
    }

    public void print() {
        for (Range range : group) {
            System.out.println(range.contents);
        }
    }
}
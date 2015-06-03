public class StringFreq implements Comparable<StringFreq> {
    public String word;
    public double count;

    public StringFreq(String x, double a) {
        word = x;
        count = a;
    }

    @Override
    public int compareTo(StringFreq o) {
        return Double.compare(this.count, o.count);
    }
}
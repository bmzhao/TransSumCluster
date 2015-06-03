import java.io.File;

/**
 * Created by brianzhao on 6/3/15.
 */
public class Driver {
    public static void main(String[] args) throws Exception {
        Summary summary = new Summary(new File("/Users/brianzhao/Google Drive/Documents/Research/462/TransSumCluster/CS144 Fall 2013 Video 1-2- The four layer Internet model.txt"), .30, 11111111, 1111111, 0, Weight.TFIDF);
    }
}

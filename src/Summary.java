import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by brianzhao on 6/2/15.
 */
public class Summary {
    //histogram represents list of all timestamped regions of the video
    private ArrayList<Range> histogram = new ArrayList<>();

    //global term frequency count for each word (total number of times word was said in entire video)
    private HashMap<String, Double> tf = new HashMap<>();

    //global document frequency count for each word (total number of time segments the word appears in)
    private HashMap<String, Double> df = new HashMap<>();

    //global tf-idf weight for each word
    private HashMap<String, Double> tfIdf = new HashMap<>();

    //list of stopwords that are not counted
    private StopWords stop = new StopWords();

    //takes input as stemmed string, returs the unstemmed string
    private HashMap<String, String> stemmedToUnstemmed = new HashMap();

    //proportion of highest tf-idf words that are considered as "important" to increment score
    private double topWords;
    private Weight weightType;
    private double cutOffValue;

    //list of all words, sorted by their global tf-idf weight in increasing order
    private ArrayList<StringFreq> sortedTFIDF = new ArrayList<>();
    private ArrayList<StringFreq> sortedTF = new ArrayList<>();

    //list of all words - used to identify the word for each index of the TFIDF vector
    private ArrayList<String> wordOrdering = new ArrayList<>();


    public Summary(File input,
                   double percentageOfTopwords,
                   double percentageOfVideo,
                   int lookAhead,
                   double cutOffValue,
                   Weight weightType) throws Exception {

        //creates the histogram of ranges
        initializeHistogram(input);


        //perform preprocessing and dictionary creation
        clean();
        //now, tf, df, tfIdf, and stemmedToUnstemmed are finalized

        createSorted();

        //topwords reflects the proportion of the highest tf-idf value words which will be deemed "important"
        this.topWords = percentageOfTopwords;
        this.weightType = weightType;
        this.cutOffValue = cutOffValue;

        SummationSummary();

    }


    public void setTopWords(double topWords) {
        this.topWords = topWords;
    }

    public void setWeightType(Weight weightType) {
        this.weightType = weightType;
    }


    private void initializeHistogram(File input) throws Exception {
        Scanner reader = new Scanner(input);
        String decider = reader.nextLine();
        reader = new Scanner(input);
        int counter = 0;
        if (decider.matches("^[0-9]*:[0-9]*$")) {
            while (reader.hasNextLine()) {
                String time = reader.nextLine();
                String contents = reader.nextLine();
                if (time.matches("^[0-9]:.*$")) {
                    time = "0" + time;
                }
                histogram.add(new Range(time, contents, counter++));
            }
        }
        //if video is over an hour , this will not work
        else {
            while (reader.hasNextLine()) {
                String[] divisions = reader.nextLine().split(":");
                String time = divisions[0] + ":" + divisions[1].substring(0, 2);
                if (time.matches("^[0-9]:.*$")) {
                    time = "0" + time;
                }
                String contents = divisions[1].substring(2, divisions[1].length());
                for (int i = 2; i < divisions.length; i++)
                    contents += " " + divisions[i];
                histogram.add(new Range(time, contents, counter++));
            }
        }
    }

    private void clean() {
        for (Range range : histogram) {
            //if (current.contents.matches("^[A-Z]*:.*"))
            //convert all words to lowercase and remove any punctuation
            String[] words = range.contents.toLowerCase().replaceAll("[^\\w ]", "").split("\\s+");

            //holder corresponds to the field "localTF" for each range object in the histogram
            HashMap<String, Double> holder = new HashMap<>();
            for (String currentWord : words) {
                //skip the word if it is a stopword
                if (stop.isStopWord(currentWord)) {
                    continue;
                }

                //stem the current word
                Stemmer s = new Stemmer();
                char[] word = currentWord.toCharArray();
                s.add(word, word.length);
                s.stem();
                String toAdd = s.toString();

                //if the stemmed word isn't inside the hashmap, add it w/a frequency of 1
                if (!holder.containsKey(toAdd)) {
                    holder.put(toAdd, 1.0);
                    //increment the word's associated document frequency as well, since this is the first time
                    //the word has occurred in this timeregion
                    if (!df.containsKey(toAdd)) {
                        df.put(toAdd, 1.0);
                        stemmedToUnstemmed.put(toAdd, currentWord);
                    } else {
                        df.put(toAdd, df.get(toAdd) + 1);
                    }
                }
                //otherwise , increment the value in the holder hashmap
                else {
                    holder.put(toAdd, holder.get(toAdd) + 1);
                }

                //increment the global tf frequency associated with this word
                if (!tf.containsKey(toAdd))
                    tf.put(toAdd, 1.0);
                else
                    tf.put(toAdd, tf.get(toAdd) + 1);
            }
            holder.remove("");
            range.localTF = holder;
        }
        tf.remove("");
        df.remove("");

        //compute all tf-idf weightings for words, and store them in hashmap tfIdf
        for (String s : tf.keySet()) {
            tfIdf.put(s, tf_idf(s));
        }

    }

    private double tf_idf(String t) {
        double tf = 1 + Math.log10(this.tf.get(t)); //log normalized tf
        double idf = Math.log10(histogram.size() / df.get(t)); //inverse freq
        return tf * idf;
    }

    private void createSorted() {
        for (String z : tfIdf.keySet()) {
            sortedTFIDF.add(new StringFreq(z, tfIdf.get(z)));
            sortedTF.add(new StringFreq(z, tf.get(z)));
            wordOrdering.add(z);
        }
        Collections.sort(sortedTFIDF);
        Collections.sort((sortedTF));
        createDocumentTFIDFVectors();
    }

    private void createDocumentTFIDFVectors() {
        int vectorLength = wordOrdering.size();
        for (Range range : histogram) {
            range.tfIdfVector = new double[vectorLength];
            for (int i = 0; i < vectorLength; i++) {
                String currentString = wordOrdering.get(i);
                if (!range.localTF.containsKey(currentString)) {
                    range.tfIdfVector[i] = 0;
                } else {
                    range.tfIdfVector[i] = (1 + Math.log10(range.localTF.get(currentString))) * (Math.log10(histogram.size() / df.get(currentString)));
                }
            }
        }
        normalizeDocumentTFIDFVectors();
    }

    private void normalizeDocumentTFIDFVectors() {
        //NORMALIZATION!!!!!!
        for (Range range : histogram) {
            double sum = 0;
            for (double a : range.tfIdfVector) {
                sum += a * a;
            }
            sum = Math.sqrt(sum + 1);
            for (int i = 0; i < range.tfIdfVector.length; i++) {
                range.tfIdfVector[i] = range.tfIdfVector[i] / sum;
            }
        }
    }

    private void SummationSummary() {

        ArrayList<StringFreq> orderToBeConsidered = new ArrayList<>();
        if (weightType == Weight.TF) {
            orderToBeConsidered = sortedTF;
        } else if (weightType == Weight.TFIDF) {
            orderToBeConsidered = sortedTFIDF;
        }

        //iterate through the timeregions of the histogram
        for (Range range : histogram) {
            //for the topMost words in the arraylist sorted, i.e. the top percentage of the tf-idf weighted words
            for (int j = (int) ((1 - topWords) * orderToBeConsidered.size()); j < orderToBeConsidered.size(); j++) {
                //if the current range localTF field contains this word, increment the range's importance by the global tf-idf weight of the word
                if (range.localTF.containsKey(sortedTFIDF.get(j).word)) {
                    double termImportance = 0;
                    if (weightType == Weight.TF) {
                        termImportance = tf.get(sortedTFIDF.get(j).word);
                    } else if (weightType == Weight.TFIDF) {
                        termImportance = tfIdf.get(sortedTFIDF.get(j).word);
                    }
                    range.importance += termImportance;
                }
            }
        }

        ArrayList<Group> condensedRegions = createGroups();
        Collections.sort(condensedRegions);
        System.out.println("Number of Groups: " + condensedRegions.size());

        for (Group group1 : condensedRegions) {
            System.out.println(group1.get(0).startTime + " - " + group1.get(group1.size() - 1).startTime);
            group1.print();
            System.out.println();
        }

        System.out.println("Sorted TFIDF values of all words in transcript: ");
        //to see sorted Tf-IDF Values
        for (int i = 0; i < sortedTFIDF.size(); i++) {
            System.out.println(stemmedToUnstemmed.get(sortedTFIDF.get(i).word) + "\t\t" + sortedTFIDF.get(i).count);
        }

        System.out.println("\n\n\n");

        System.out.println("Sorted TFIDF values of all words in transcript: ");
        //to see sorted TF-values
        for (int i = 0; i < sortedTFIDF.size(); i++) {
            System.out.println(stemmedToUnstemmed.get(sortedTF.get(i).word) + "\t\t" + sortedTF.get(i).count);
        }

    }


    private ArrayList<Group> createGroups() {
        boolean inWord = false;
        ArrayList<Group> groups = new ArrayList<>();
        Group group = null;
        int histogramSize = histogram.size();
        for (int i = 0; i < histogramSize; i++) {
            Range range = histogram.get(i);
            //if the importance is greater than the cutOffValue,
            if (range.importance > cutOffValue) {
                if (!inWord) {
                    inWord = true;
                    group = new Group();
                    group.add(range);
                } else {
                    group.add(range);
                }
                if (i == histogramSize - 1) {
                    inWord = false;
                    groups.add(group);
                }
            } else {
                if (inWord) {
                    inWord = false;
                    groups.add(group);
                }
            }
        }
        return groups;
    }




}

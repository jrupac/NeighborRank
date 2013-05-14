import cern.colt.map.OpenIntDoubleHashMap;
import cern.colt.map.OpenIntObjectHashMap;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.util.*;

/**
 * An implementation of the naive NN-Descent algorithm from the paper.
 *
 * Some credit to: http://stackoverflow.com/questions/1844194/get-cosine-similarity-between-two-documents-in-lucene
 *
 * Usage: java NNDescent </path/to/index/> </path/to/output/file> K
 */
public class NNDescent {
    private static final double DELTA = 0.05;

    private static final OpenIntDoubleHashMap cachedSims = new
            OpenIntDoubleHashMap();
    private static final OpenIntObjectHashMap cachedTermFreqs = new
            OpenIntObjectHashMap();
    private static final Random rand = new Random(System.nanoTime());

    private static int N;

    /**
     * Variation of Carmack's Fast Inverse Square Root for double-precision
     * Credit to: http://stackoverflow.com/questions/11513344/how-to-implement-the-fast-inverse-square-root-in-java
     * @param x Input value
     * @return ~1/sqrt(x)
     */
    public static double invSqrt(double x) {
        double xhalf = 0.5d * x;
        long i = Double.doubleToLongBits(x);
        i = 0x5fe6ec85e7de30daL - (i >> 1);
        x = Double.longBitsToDouble(i);
        x = x * (1.5d - xhalf * x * x);
        return x;
    }

    public static void setN(int n) {
        N = n;
    }

    @SuppressWarnings(value = "unchecked")
    public static double getCosineSimilarity(IndexReader reader, int doc1, int doc2) {
        // Don't want doc being a neighbor of itself
        if (doc1 == doc2) {
            return 0.0;
        }

        Double cached = null;

        if (doc1 < doc2) {
            if (cachedSims.containsKey((doc1) * N + (doc2))) {
                cached = cachedSims.get((doc1) * N + (doc2));
            }
        } else {
            if (cachedSims.containsKey((doc2) * N + (doc1))) {
                cached = cachedSims.get((doc2) * N + (doc1));
            }
        }

        if (cached != null) {
            return cached;
        }


        Map<String, Double> f1 = (Map<String, Double>) cachedTermFreqs.get(doc1);
        Map<String, Double> f2 = (Map<String, Double>) cachedTermFreqs.get(doc2);

        try {
            if (f1 == null) {
                f1 = new TreeMap<String, Double>();
                getTermFrequencies(reader, f1, doc1);
                cachedTermFreqs.put(doc1, f1);
            }

            if (f2 == null) {
                f2 = new TreeMap<String, Double>();
                getTermFrequencies(reader, f2, doc2);
                cachedTermFreqs.put(doc2, f2);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("ERROR: Cannot get term vectors.");
            return 0.0;
        }

        double dotProduct = 0;
        double v1Norm = 0, v2Norm = 0;

        // L1-norm each vector
        double l1norm = 0.0;
        for (Double value: f1.values()) {
            l1norm += value;
        }
        for (String keys : f1.keySet()) {
            f1.put(keys, f1.get(keys) / l1norm);
        }

        l1norm = 0.0;
        for (Double value: f2.values()) {
            l1norm += value;
        }
        for (String keys : f2.keySet()) {
            f2.put(keys, f2.get(keys) / l1norm);
        }

        for (Map.Entry<String, Double> entry : f1.entrySet()) {
            String term = entry.getKey();
            Double fr1 = entry.getValue(), fr2 = f2.get(term);

            if (fr2 != null) {
                dotProduct += fr1 * fr2;
            }
            v1Norm += fr1 * fr1;
        }

        for (Double value: f2.values()) {
            v2Norm += value * value;
        }

        double sim = dotProduct * invSqrt(v1Norm * v2Norm);

        if (doc1 < doc2) {
            cachedSims.put((doc1) * N + (doc2), sim);
        } else {
            cachedSims.put((doc2) * N + (doc1), sim);
        }

        return sim;
    }

    private static void getTermFrequencies(IndexReader reader,
                                           Map<String, Double> f, int docId)
            throws IOException {
        Fields fields = reader.getTermVectors(docId);

        for (String field : fields) {
            Terms vector = fields.terms(field);
            TermsEnum termsEnum = null;
            termsEnum = vector.iterator(termsEnum);
            BytesRef text = null;
            while ((text = termsEnum.next()) != null) {
                String term = text.utf8ToString();
                int freq = (int) termsEnum.totalTermFreq();
                f.put(term, (double) freq);
            }
        }
    }

    private static List<List<Integer>> reverse(List<FixedSizePriorityQueue<Neighbor>> neighborLists) {
        int N = neighborLists.size();
        List<List<Integer>> reversedLists = new ArrayList<List<Integer>>(N);
        for (int i = 0; i < N; i++) {
            reversedLists.add(new ArrayList<Integer>());
        }

        for (int i = 0; i < N; i++) {
            FixedSizePriorityQueue<Neighbor> neighbors = neighborLists.get(i);
            for (Neighbor n : neighbors) {
                reversedLists.get(n.id).add(i);
            }
        }

        return reversedLists;
    }

    private static class Neighbor implements Comparable<Neighbor> {
        public final int id;
        public double sim;

        public Neighbor(int id, double sim) {
            this.id = id;
            this.sim = sim;
        }

        @Override
        public int compareTo(Neighbor n) {
            if (this.sim < n.sim)
                return -1;
            else if (this.sim > n.sim)
                return 1;
            else
                return this.id - n.id;
        }
    }

    /**
     * Shuffle the first K elements of arr
     * @param arr
     * @param K
     */
    private static void shuffle(int[] arr, int K) {
        for (int i = 0; i < K; i++) {
            int index = i + rand.nextInt(arr.length - i);
            int temp = arr[i];
            arr[i] = arr[index];
            arr[index] = temp;
        }
    }

    public static List<FixedSizePriorityQueue<Neighbor>> run(IndexReader reader, int K) {
        setN(reader.maxDoc());

        int threshold = (int) (DELTA * N * K);

        List<Integer> docIds = new ArrayList<Integer>(N);
        int[] shuffledIds = new int[N];
        for (int i = 0; i < N; i++) {
            docIds.add(i);
            shuffledIds[i] = i;
        }

        List<FixedSizePriorityQueue<Neighbor>> neighborLists = new ArrayList<FixedSizePriorityQueue<Neighbor>>(N);

        // B[v] <- Sample(V, K) x {infty}
        for (int i = 0; i < N; i++) {
            neighborLists.add(new FixedSizePriorityQueue<Neighbor>(K));
            shuffle(shuffledIds, K);

            for (int j = 0; j < K; j++) {
                neighborLists.get(i).add(new Neighbor(shuffledIds[j], Double.NEGATIVE_INFINITY));
            }
        }

        // loop
        while (true) {
            // R <- Reverse(B)
            List<List<Integer>> reversedNeighbors = reverse(neighborLists);

            // B'[v] <- B[v] U R[v]
            List<List<Integer>> extendedNeighbors = new ArrayList<List<Integer>>(N);
            for (int i = 0; i < N; i++) {
                List<Integer> ext = new ArrayList<Integer>();
                for (Neighbor n : neighborLists.get(i)) {
                    ext.add(n.id);
                }
                ext.addAll(reversedNeighbors.get(i));
                extendedNeighbors.add(ext);
            }

            // c <- 0
            int counter = 0;

            // for v \in V
            for (int v = 0; v < N; v++) {
                // for u1 \in B'[v]
                for (Integer u1 : extendedNeighbors.get(v)) {
                    // for u2 \in B'[u1]
                    for (Integer u2 : extendedNeighbors.get(u1)) {
                        // l <- \sigma(v, u2)
                        double sim = getCosineSimilarity(reader, v, u2);
                        // c <- c + UpdateNN(B[v], <u2, l>)
                        if (neighborLists.get(v).add(new Neighbor(u2, sim))) {
                            counter++;
                        }
                    }
                }
            }

            System.out.println(counter);
            if (counter < threshold) {
                break;
            }
        }

        return neighborLists;

        // TODO: Test this.
    }

    private static void writeResults(List<FixedSizePriorityQueue<Neighbor>> neighborLists,
                                     File outputFile, int N, int K) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));

        out.println(N + " " + K);

        for (FixedSizePriorityQueue<Neighbor> nnList : neighborLists) {
            for (Neighbor n : nnList.descendingSet()) {
                out.print(n.id + " ");
            }
            out.println();
        }

        out.flush();
    }

    public static void main(String[] args) {

        DirectoryReader reader;

        // Load index from disk:
        Directory directory = null;
        try {
            directory = FSDirectory.open(new File(args[0]));
        } catch (IOException e) {
            System.err.println("FATAL: Cannot open index file. Exiting");
            return;
        }

        // Open the index:
        try {
            reader = DirectoryReader.open(directory);
        } catch (IOException e) {
            System.err.println("FATAL: Could not open the directory for reading. Exiting.");
            return;
        }

        File outputFile = new File(args[1]);
        int K = Integer.parseInt(args[2]);

        List<FixedSizePriorityQueue<Neighbor>> neighbors = run(reader, K);

        try {
            writeResults(neighbors, outputFile, neighbors.size(), K);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

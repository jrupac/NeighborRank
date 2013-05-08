import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * An implementation of the naive NN-Descent algorithm from the paper.
 *
 * Some credit to: http://stackoverflow.com/questions/1844194/get-cosine-similarity-between-two-documents-in-lucene
 *
 * Usage: java NNDescent </path/to/index/> K
 */
public class NNDescent {
    private static double getCosineSimilarity(IndexReader reader, int doc1, int doc2) {
        Set<String> terms = new HashSet<String>();
        Map<String, Integer> f1 = null;
        Map<String, Integer> f2 = null;
        try {
            f1 = getTermFrequencies(reader, terms, doc1);
            f2 = getTermFrequencies(reader, terms, doc2);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("ERROR: Cannot get term vectors.");
            return 0.0;
        }

        RealVector v1 = toRealVector(f1, terms);
        RealVector v2 = toRealVector(f2, terms);
        return (v1.dotProduct(v2)) / (v1.getNorm() * v2.getNorm());
    }

    private static Map<String, Integer> getTermFrequencies(IndexReader reader, Set<String> terms, int docId)
            throws IOException {
        Map<String, Integer> frequencies = new HashMap<String, Integer>();
        Fields fields = reader.getTermVectors(docId);

        for (String field : fields) {
            Terms vector = fields.terms(field);
            TermsEnum termsEnum = null;
            termsEnum = vector.iterator(termsEnum);
            BytesRef text = null;
            while ((text = termsEnum.next()) != null) {
                String term = text.utf8ToString();
                int freq = (int) termsEnum.totalTermFreq();
                frequencies.put(term, freq);
                terms.add(term);
            }
        }

        return frequencies;
    }

    private static RealVector toRealVector(Map<String, Integer> map, Set<String> terms) {
        RealVector vector = new ArrayRealVector(terms.size());
        int i = 0;
        for (String term : terms) {
            int value = map.containsKey(term) ? map.get(term) : 0;
            vector.setEntry(i++, value);
        }
        return (RealVector) vector.mapDivide(vector.getL1Norm());
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
                return 0;
        }
    }

    public static void run(/*Analyzer analyzer, */Directory directory, int K) {
        // Open the index:
        DirectoryReader reader = null;
        try {
            reader = DirectoryReader.open(directory);
        } catch (IOException e) {
            System.err.println("FATAL: Could not open the directory for reading. Exiting.");
            return;
        }

        int N = reader.maxDoc();

        List<Integer> docIds = new ArrayList<Integer>(N);
        for (int i = 0; i < N; i++) {
            docIds.add(i);
        }
        RandomDataGenerator rand = new RandomDataGenerator();
        List<FixedSizePriorityQueue<Neighbor>> neighborLists = new ArrayList<FixedSizePriorityQueue<Neighbor>>(N);

        for (int i = 0; i < N; i++) {
            neighborLists.add(new FixedSizePriorityQueue<Neighbor>(K));
            // TODO: This is not great, but a node shouldn't sample itself. Or can it?
            docIds.set(i, rand.nextInt(0, N - 1));
            for (Object neighbor : rand.nextSample(docIds, K)) {
                neighborLists.get(i).add(new Neighbor((Integer) neighbor, Double.POSITIVE_INFINITY));
            }
            docIds.set(i, i);
        }

        while (true) {
            List<List<Integer>> reversedNeighbors = reverse(neighborLists);
            List<List<Integer>> extendedNeighbors = new ArrayList<List<Integer>>(N);
            for (int i = 0; i < N; i++) {
                List<Integer> ext = new ArrayList<Integer>();
                for (Neighbor n : neighborLists.get(i)) {
                    ext.add(n.id);
                }
                ext.addAll(reversedNeighbors.get(i));
                extendedNeighbors.add(ext);
            }

            int counter = 0;

            for (int v = 0; v < N; v++) {
                for (Integer u1 : extendedNeighbors.get(v)) {
                    for (Integer u2 : extendedNeighbors.get(u1)) {
                        double sim = getCosineSimilarity(reader, v, u2);
                        if (neighborLists.get(v).add(new Neighbor(u2, sim))) {
                            counter++;
                        }
                    }
                }
            }

            if (counter == 0) {
                break;
            }
        }

        // TODO: Return instead of break.
        // TODO: Test this.
    }

    public static void main(String[] args) {
        // Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);

        // Load index from disk:
        File indexFile = new File(args[0]);
        Directory directory = null;
        try {
            directory = FSDirectory.open(indexFile);
        } catch (IOException e) {
            System.err.println("FATAL: Cannot open index file. Exiting");
            return;
        }

        NNDescent.run(/*analyzer, */directory, Integer.parseInt(args[1]));
    }
}

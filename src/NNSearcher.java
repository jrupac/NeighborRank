import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Loads a nearest neighbor list and allows users to find neighbors
 * of a document.
 */
public class NNSearcher {

    private static DirectoryReader reader;
    private static int[][] nnList;
    private static int N, K;

    public static void setIndex(String indexPath) {
        Directory directory = null;
        try {
            directory = FSDirectory.open(new File(indexPath));
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
    }

    public static void setNNList(String nnPath) throws FileNotFoundException {
        Scanner scan = new Scanner(new File(nnPath));

        N = scan.nextInt();
        K = scan.nextInt();

        NNDescent.setN(N);
        nnList = new int[N][K];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < K; j++) {
                nnList[i][j] = scan.nextInt();
            }
        }

        scan.close();
    }

    // Get list of docs with IDs, titles, and summaries
    public static List<Doc> getNeighbors(int doc) {
        List<Doc> neighbors = new ArrayList<Doc>(K);

        for (int i = 0; i < K; i++) {
            int id = nnList[doc][i];

            Document d = null;
            try {
                d = reader.document(id);
            } catch (IOException e) {
                System.err.println("Cannot find nearest neighbors for document " + doc + ". Skipping...");
            }

            neighbors.add(new Doc(id, d.get("title"), d.get("summary")));
        }

        return neighbors;
    }
}

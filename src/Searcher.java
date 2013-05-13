import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic search client to test that index was created.
 *
 * Usage: java Searcher </path/to/index/> <query>
 */
public class Searcher {

    private static final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
    private static final QueryParser parser = new QueryParser(Version.LUCENE_43, "summary", analyzer);

    private static Directory directory;
    private static DirectoryReader ireader;
    private static IndexSearcher isearcher;

    public static void setIndex(String indexPath) {
        // Load index from disk:
        File indexFile = new File(indexPath);
        try {
            directory = FSDirectory.open(indexFile);
        } catch (IOException e) {
            System.err.println("FATAL: Cannot open index file. Exiting");
            return;
        }

        try {
            ireader = DirectoryReader.open(directory);
        } catch (IOException e) {
            System.err.println("FATAL: Could not open the directory for reading. Exiting.");
            return;
        }

        isearcher = new IndexSearcher(ireader);
    }

    public static void closeIndex() {
        try {
            ireader.close();
            directory.close();
        } catch (IOException e) {
            System.err.println("ERROR: Failed to close successfully.");
        }
    }

    public static List<Doc> search(String text, int M) {
        List<Doc> results = new ArrayList<Doc>();

        // Parse a simple query that searches for "text":
        Query query = null;
        try {
            query = parser.parse(text);
        } catch (ParseException e) {
            System.err.println("ERROR: Cannot parse query.");
            return results;
        }
        ScoreDoc[] hits = new ScoreDoc[0];
        try {
            hits = isearcher.search(query, M).scoreDocs;
        } catch (IOException e) {
            System.err.println("ERROR: Failed to execute search.");
        }

        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            try {
                Document hitDoc = isearcher.doc(hits[i].doc);
                results.add(new Doc(hits[i].doc, hitDoc.get("title"), hitDoc.get("summary")));
            } catch (IOException e) {
                System.err.println("ERROR: Failed to convert search result to document.");
            }
            // assertEquals("This is the text to be indexed.", hitDoc.get("fieldname"));
        }

        return results;
    }

    public static void main(String[] args) {
        Searcher.setIndex(args[0]);
        Searcher.search(args[1], 100);
    }
}

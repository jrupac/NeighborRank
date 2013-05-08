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

/**
 * Basic search client to test that index was created.
 *
 * Usage: java IndexSearcher </path/to/index/>
 */
public class Searcher {
    public static void search(Analyzer analyzer, Directory directory) {
        // Now search the index:
        DirectoryReader ireader = null;
        try {
            ireader = DirectoryReader.open(directory);
        } catch (IOException e) {
            System.err.println("FATAL: Could not open the directory for reading. Exiting.");
            return;
        }
        IndexSearcher isearcher = new IndexSearcher(ireader);
        // Parse a simple query that searches for "text":
        QueryParser parser = new QueryParser(Version.LUCENE_43, "summary", analyzer);
        Query query = null;
        try {
            query = parser.parse("quantum");
        } catch (ParseException e) {
            System.err.println("ERROR: Cannot parse query.");
            return;
        }
        ScoreDoc[] hits = new ScoreDoc[0];
        try {
            hits = isearcher.search(query, null, 1000).scoreDocs;
        } catch (IOException e) {
            System.err.println("ERROR: Failed to execute search.");
        }
        // assertEquals(1, hits.length);
        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            try {
                Document hitDoc = isearcher.doc(hits[i].doc);
                System.out.println(hitDoc.get("title"));
            } catch (IOException e) {
                System.err.println("ERROR: Failed to convert search result to document.");
            }
            // assertEquals("This is the text to be indexed.", hitDoc.get("fieldname"));
        }

        try {
            ireader.close();
            directory.close();
        } catch (IOException e) {
            System.err.println("ERROR: Failed to close successfully.");
        }
    }

    public static void main(String[] args) {
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);

        // Load index from disk:
        File indexFile = new File(args[0]);
        Directory directory = null;
        try {
            directory = FSDirectory.open(indexFile);
        } catch (IOException e) {
            System.err.println("FATAL: Cannot open index file. Exiting");
            return;
        }

        Searcher.search(analyzer, directory);
    }
}

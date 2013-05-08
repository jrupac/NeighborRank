import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * Reads parsed files from the input folder and creates a Lucene index
 * in memory.
 *
 * Usage: java IndexCreator </path/to/parsed/data/> </path/to/index/>
 */
public class IndexCreator {
    public static void main(String[] args) {
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);

        // Store the index in memory:
        // Directory directory = new RAMDirectory();

        // To store an index on disk:
        File indexFile = new File(args[1]);
        Directory directory = null;
        try {
            directory = FSDirectory.open(indexFile);
        } catch (IOException e) {
            System.err.println("FATAL: Cannot open index file. Exiting");
            return;
        }
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);

        IndexWriter iwriter = null;
        try {
            iwriter = new IndexWriter(directory, config);
        } catch(IOException e) {
            System.err.println("FATAL: Cannot create index. Exiting.");
            return;
        }

        File dataDir = new File(args[0]);

        for (File f : dataDir.listFiles()) {
            Scanner s = null;

            try {
                s = new Scanner(f);
            } catch (FileNotFoundException e) {
                System.err.println("ERROR: Could not find file " + f.getName() + ". Skipping.");
                continue;
            }

            while (s.hasNext()) {
                Document doc = new Document();
                String title = s.nextLine();

                // TODO: fix parsing instead of doing this
                if (!s.hasNext())
                    continue;
                String summary = s.nextLine();
                doc.add(new Field("title", title, TextField.TYPE_STORED));
                doc.add(new Field("summary", summary, TextField.TYPE_STORED));

                try {
                    iwriter.addDocument(doc);
                } catch (IOException e) {
                    System.err.println("ERROR: Could not add '" + title + "' to the index. Skipping.");
                }
            }
        }

        try {
            iwriter.close();
        } catch (IOException e) {
            System.err.println("ERROR: Did not close index successfully.");
        }
    }
}

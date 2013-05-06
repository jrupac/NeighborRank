import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Reads all files in given directory and outputs the <title /> and <summary /> of each <entry />
 * block to stdout.
 *
 * Usage: java DataParser <input folder> <output folder> <total number of files to parse>
 */
public class DataParser {
    public static void main(String args[]) {
        final String outputDir = args[1];
        int limit = Integer.parseInt(args[2]);

        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }

        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

            File inputDir = new File(args[0]);

            if (inputDir.isDirectory()) {
                File[] files = inputDir.listFiles();

                if (files != null) {
                    int numFilesToParse = Math.min(files.length, limit);

                    for (int i = 0; i < numFilesToParse; i++) {
                        File file = files[i];

                        System.err.println("Parsing file: " + file.getName());

                        File outputFile = new File(outputDir, file.getName());
                        saxParser.parse(file, new ArXiVHandler(outputFile));
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            System.err.println("Unable to load parser: " + e.getMessage());
        } catch (SAXException e) {
            System.err.println("Unable to parse XML: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Unable to read input: " + e.getMessage());
        }
    }

    private static class ArXiVHandler extends DefaultHandler {
        private static final String SEPARATOR = System.getProperty("line.separator");

        private final PrintWriter printWriter;

        private boolean inEntry = false;
        private boolean inTitle = false;
        private boolean inSummary = false;
        private StringBuilder builder = new StringBuilder();

        public ArXiVHandler(File outputFile) throws FileNotFoundException {
            printWriter = new PrintWriter(outputFile);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equalsIgnoreCase("entry")) {
                inEntry = true;
            } else if (inEntry && qName.equalsIgnoreCase("title")) {
                inTitle = true;
            } else if (inEntry && qName.equalsIgnoreCase("summary")) {
                inSummary = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inEntry && (inTitle || inSummary)) {
                builder.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (inTitle && qName.equalsIgnoreCase("title")) {
                inTitle = false;
                printWriter.write(builder.toString().replaceAll(SEPARATOR, " ").trim());
                printWriter.write(SEPARATOR);
                builder = new StringBuilder();
            }
            if (inSummary && qName.equalsIgnoreCase("summary")) {
                inSummary = false;
                printWriter.write(builder.toString().replaceAll(SEPARATOR, " ").trim());
                printWriter.write(SEPARATOR);
                builder = new StringBuilder();
            }
            if (inEntry && qName.equalsIgnoreCase("entry")) {
                inEntry = false;
            }
        }
    }
}

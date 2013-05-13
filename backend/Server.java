import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * API server.
 *
 * Usage: java Server </path/to/index/> </path/to/NNList>
 */
public class Server {

    public static void main(String args[]) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(8080);
        HttpServer server = HttpServer.create(addr, 0);

        Searcher.setIndex(args[0]);
        NNSearcher.setIndex(args[0]);
        NNSearcher.setNNList(args[1]);

        server.createContext("/results", new ResultsHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.err.println("Server starting...");
    }
}

class ResultsHandler implements HttpHandler {

    private static final int MAX_RESULTS = 30;
    private static final JSONParser PARSER = new JSONParser(JSONParser.MODE_JSON_SIMPLE);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String uri = exchange.getRequestURI().getPath();

        Headers responseHeader = exchange.getResponseHeaders();

        responseHeader.set("Content-Type", "application/json");
        responseHeader.set("Access-Control-Allow-Origin", "*");

        OutputStream response = exchange.getResponseBody();

        String content = "";
        Object object = null;
        try {
            object = PARSER.parse(exchange.getRequestBody());
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        }
        if (!(object instanceof JSONObject)) {
            exchange.sendResponseHeaders(200, content.length());
            response.close();
            return;
        }

        JSONObject map = (JSONObject) object;

        String query = (String) map.get("query");

        Object mObject = map.get("M");
        int M = 0;
        if (mObject instanceof Number) {
            M = ((Number) mObject).intValue();
        } else if (mObject instanceof String) {
            M = Integer.parseInt((String) mObject);
        }
        M = Math.min(M, MAX_RESULTS);

        Object kObject = map.get("K");
        int K = 0;
        if (kObject instanceof Number) {
            K = ((Number) kObject).intValue();
        } else if (kObject instanceof String) {
            K = Integer.parseInt((String) kObject);
        }

        List<Doc> luceneResults = Searcher.search(query, M);

        JSONArray resultsArray = new JSONArray();

        int totalResults = 0;
        JSONObject docObj;

        for (Doc d : luceneResults) {
            if (totalResults++ >= MAX_RESULTS) {
                break;
            }

            // First display the result given by the vector model
            docObj = new JSONObject();
            docObj.put("docid", d.getId());
            docObj.put("title", d.getTitle());
            docObj.put("summary", d.getSummary());
            resultsArray.add(docObj);

            // Then show the nearest neighbors to that result
            List<Doc> neighbors = NNSearcher.getNeighbors(d.getId());
            for (int i = 0; i < K && totalResults < MAX_RESULTS; i++, totalResults++) {
                docObj = new JSONObject();
                Doc n = neighbors.get(i);
                docObj.put("docid", n.getId());
                docObj.put("title", n.getTitle());
                docObj.put("summary", n.getSummary());
                resultsArray.add(docObj);
            }
        }

        // Return results as json
        JSONObject responseObject = new JSONObject();
        responseObject.put("results", resultsArray);

        content = responseObject.toString();

        exchange.sendResponseHeaders(200, content.length());
        response.write(content.getBytes());
        response.close();
    }
}

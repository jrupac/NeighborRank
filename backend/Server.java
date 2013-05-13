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
 * Usage: java Server </path/to/index/>
 */
public class Server {

    public static void main(String args[]) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(8080);
        HttpServer server = HttpServer.create(addr, 0);

        Searcher.setIndex(args[0]);

        server.createContext("/results", new ResultsHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.err.println("Server starting...");
    }
}

class ResultsHandler implements HttpHandler {

    private static final JSONParser PARSER = new JSONParser(JSONParser.MODE_JSON_SIMPLE);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String uri = exchange.getRequestURI().getPath();
        System.out.println(uri);
        System.out.println("\t" + exchange.getRequestURI().getQuery());

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

        Object kObject = map.get("K");
        int K = 0;
        if (mObject instanceof Number) {
            K = ((Number) mObject).intValue();
        } else if (mObject instanceof String) {
            K = Integer.parseInt((String) mObject);
        }

        List<Doc> results = Searcher.search(query, M);

        JSONArray resultsArray = new JSONArray();

        for (Doc d : results) {
            JSONObject docObj = new JSONObject();
            docObj.put("docid", d.getId());
            docObj.put("title", d.getTitle());
            docObj.put("summary", d.getSummary());
            resultsArray.add(docObj);
        }

        JSONObject responseObject = new JSONObject();
        responseObject.put("results", resultsArray);

        content =
                "{" +
                        "    \"results\": [{\n" +
                        "    \"docid\": 1,\n" +
                        "    \"title\": \"An analogue of the Szemeredi Regularity Lemma for bounded degree graphs\",\n" +
                        "    \"summary\": \"We show that a sufficiently large graph of bounded degree can" +
                        " be decomposed into quasi-homogeneous pieces. The result can be viewed as a 'finitarization' of the classical Farrell-Varadarajan Ergodic Decomposition Theorem.\",\n" +
                        "    },\n" +
                        "    {\n" +
                        "    \"docid\": 2,\n" +
                        "    \"title\": \"Drift-diffusion model for spin-polarized transport in a non-degenerate 2DEG controlled by a spin-orbit interaction\",\n" +
                        "    \"summary\": \"We apply the Wigner function formalism to derive drift-diffusion transport equations for spin-polarized electrons in a III-V semiconductor single quantum well. Electron spin dynamics is controlled by the linear in momentum spin-orbit interaction. In a studied transport regime an electron momentum scattering rate is appreciably faster than spin dynamics. A set of transport equations is defined in terms of a particle density, spin density, and respective fluxes. The developed model allows studying of coherent dynamics of a non-equilibrium spin polarization. As an example, we consider a stationary transport regime for a heterostructure grown along the (0, 0, 1) crystallographic direction. Due to the interplay of the Rashba and Dresselhaus spin-orbit terms spin dynamics strongly depends on a transport direction. The model is consistent with results of pulse-probe measurement of spin coherence in strained semiconductor layers. It can be useful for studying properties of spin-polarized transport and modeling of spintronic devices operating in the diffusive transport regime.\",\n" +
                        "    },\n" +
                        "    {\n" +
                        "    \"docid\": 3,\n" +
                        "    \"title\": \"Optical conductivity of a quasi-one-dimensional system with fluctuating order\",\n" +
                        "    \"summary\": \"We describe a formally exact method to calculate the optical conductivity of a one-dimensional system with fluctuating order. For classical phase fluctuations we explicitly determine the optical conductivity by solving two coupled Fokker-Planck equations numerically. Our results differ considerably from perturbation theory and in contrast to Gaussian order parameter fluctuations show a strong dependence on the correlation length.\",\n" +
                        "    }]" +
                        "}";

        content =
                "{\"results\":" +
                "[{" +
                "\"docid\":1, " +
                "\"summary\":\"We show that a sufficiently large graph of bounded degree can be decomposed into quasi-homogeneous pieces. The result can be viewed as a 'finitarization' of the classical Farrell-Varadarajan Ergodic Decomposition Theorem.\"," +
                "\"title\":\"An analogue of the Szemeredi Regularity Lemma for bounded degree graphs\"" +
                "}," +
                "{" +
                "\"docid\": 2," +
                "\"title\": \"Drift-diffusion model for spin-polarized transport in a non-degenerate 2DEG controlled by a spin-orbit interaction\"," +
                "\"summary\": \"We apply the Wigner function formalism to derive drift-diffusion transport equations for spin-polarized electrons in a III-V semiconductor single quantum well. Electron spin dynamics is controlled by the linear in momentum spin-orbit interaction. In a studied transport regime an electron momentum scattering rate is appreciably faster than spin dynamics. A set of transport equations is defined in terms of a particle density, spin density, and respective fluxes. The developed model allows studying of coherent dynamics of a non-equilibrium spin polarization. As an example, we consider a stationary transport regime for a heterostructure grown along the (0, 0, 1) crystallographic direction. Due to the interplay of the Rashba and Dresselhaus spin-orbit terms spin dynamics strongly depends on a transport direction. The model is consistent with results of pulse-probe measurement of spin coherence in strained semiconductor layers. It can be useful for studying properties of spin-polarized transport and modeling of spintronic devices operating in the diffusive transport regime.\"" +
                "}" +
                "]}";

        content = responseObject.toString();

        exchange.sendResponseHeaders(200, content.length());
        response.write(content.getBytes());
        response.close();
    }
}

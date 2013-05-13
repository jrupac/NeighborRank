import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String args[]) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(8080);
        HttpServer server = HttpServer.create(addr, 0);

        server.createContext("/results", new ResultsHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.err.println("Server starting...");
    }
}

class ResultsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String uri = exchange.getRequestURI().getPath();
        System.out.println(uri);

        Headers responseHeader = exchange.getResponseHeaders();

        String content =
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
                "[{\"docid\":1, " +
                    "\"summary\":\"We show that a sufficiently large graph of bounded degree can " +
                        "be decomposed into quasi-homogeneous pieces. The result can be viewed" +
                        " as a 'finitarization' of the classical Farrell-Varadarajan Ergodic Decomposition Theorem.\"," +
                    "\"title\":\"An analogue of the Szemeredi Regularity Lemma for bounded degree graphs\"" +
                "}" +
                "]}";

        responseHeader.set("Content-Type", "application/json");
        responseHeader.set("Access-Control-Allow-Origin", "*");

        exchange.sendResponseHeaders(200, content.length());
        OutputStream response = exchange.getResponseBody();
        response.write(content.getBytes());
        response.close();
    }
}

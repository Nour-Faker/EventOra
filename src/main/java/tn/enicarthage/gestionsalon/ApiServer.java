package tn.enicarthage.gestionsalon;

import com.sun.net.httpserver.HttpServer;

import tn.enicarthage.gestionsalon.router.Router;
import tn.enicarthage.gestionsalon.util.JwtUtil;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ApiServer {

    private final HttpServer server;

    public ApiServer(int port) throws IOException {
        JwtUtil.verifierConfiguration();
        Router router = new Router();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", router::dispatcher);
        this.server.setExecutor(null);
    }

    public void demarrer() {
        server.start();
    }

    public void arreter() {
        server.stop(0);
    }
}
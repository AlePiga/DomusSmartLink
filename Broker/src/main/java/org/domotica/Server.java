package org.domotica;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class Server {
    public static void main(String[] args) throws Exception {
        Database.init();
        BrokerMQTTT.connetti();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/led", new HandlerLed());
        server.createContext("/sensori", new HandlerStart());
        server.createContext("/ventilatore", new HandlerVentilatore());

        // MULTI THREAD
        //server.setExecutor(Executors.newCachedThreadPool());

        server.start();

        System.out.println("[SERVER] Server avviato sulla porta 8080!");
    }
}

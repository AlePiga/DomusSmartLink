package org.domotica;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

class HandlerStart implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            try {
                var stmt = Database.conn.createStatement();
                var rs = stmt.executeQuery("SELECT * FROM sensori ORDER BY type");
                System.out.println(rs.getString("nome"));

                StringBuilder json = new StringBuilder();
                json.append("[");

                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    json.append("{")
                            .append("\"id\":\"").append(rs.getInt("id")).append("\",")
                            .append("\"nome\":").append(rs.getString("nome")).append(",")
                            .append("\"status\":\"").append(rs.getInt("status")).append("\",")
                            .append("\"type\":").append(rs.getString("type"))
                            .append("}");

                    first = false;
                }

                json.append("]");

                byte[] response = json.toString().getBytes();

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);

                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

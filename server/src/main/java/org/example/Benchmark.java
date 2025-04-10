package org.example;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class Benchmark {

    public static void main(String[] args) {
        final String host = "localhost";
        final int port = 8080;
        final int ops = 10000;

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            // Use a cachebase first (optional)
            out.write("USE benchmarkdb\n");
            out.flush();
            in.readLine(); // discard response

            // Benchmark SET
            long setStart = System.nanoTime();
            for (int i = 0; i < ops; i++) {
                String key = "key" + i;
                String value = UUID.randomUUID().toString();
                out.write("SET " + key + " " + value + "\n");
                out.flush();
                in.readLine(); // read "OK"
            }
            long setEnd = System.nanoTime();
            double setDurationMs = (setEnd - setStart) / 1_000_000.0;

            // Benchmark GET
            long getStart = System.nanoTime();
            for (int i = 0; i < ops; i++) {
                String key = "key" + i;
                out.write("GET " + key + "\n");
                out.flush();
                in.readLine(); // read value
            }
            long getEnd = System.nanoTime();
            double getDurationMs = (getEnd - getStart) / 1_000_000.0;

            System.out.printf("SET  %d keys in %.2f ms (%.2f ms/op)\n", ops, setDurationMs, setDurationMs / ops);
            System.out.printf("GET  %d keys in %.2f ms (%.2f ms/op)\n", ops, getDurationMs, getDurationMs / ops);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

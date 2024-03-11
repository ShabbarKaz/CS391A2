
/** Web server program
 *
 *  @author Jack Halm, Alexander Johnston, Shabbar Kazmi
 *
 *  @version CS 391 - Fall 2024 - A3
 **/

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;

public class A3 {
    static ServerSocket serverSocket = null; // listening socket
    static int portNumber = 5555; // port on which server listens
    static Socket clientSocket = null; // socket to a client

    /*
     * Start the server then repeatedly wait for a connection request, accept,
     * and start a new thread to service the request
     */
    public static void main(String args[]) {
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("%% Server started: " + serverSocket);
            while (true) {
                System.out.println("%% Waiting for a client...");
                clientSocket = serverSocket.accept();
                System.out.println("%% New connection established: " + clientSocket);
                new Thread(new WebServer(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println(
                    "There was an error opening the server socket: " +
                            e.getMessage());
        }
    }// main method
}// A3 class

class WebServer implements Runnable {
    static int numConnections = 0; // number of ongoing connections
    Socket clientSocket = null; // socket to client
    BufferedReader in = null; // input stream from client
    DataOutputStream out = null; // output stream to client

    /*
     * Store a reference to the client socket, update and display the
     * number of connected clients, and open I/O streams
     **/
    WebServer(Socket clientSocket) {
        try {
            numConnections++;
            System.out.println("%% [# of connected clients: " + numConnections + "]");
            this.clientSocket = clientSocket;
            openStreams(clientSocket);
        } catch (IOException e) {
            System.out.println(
                    "There was a problem opening streams: " +
                            e.getMessage());
        }
    }// constructor

    /*
     * Each WebServer thread processes one HTTP GET request and
     * then closes the connection
     **/
    public void run() {
        processRequest();
        close();

    }// run method

    /*
     * Parse the request then send the appropriate HTTP response
     * making sure to handle all of the use cases listed in the A3
     * handout, namely codes 200, 404, 418, 405, and 503 responses.
     **/
    void processRequest() {
        try {
            String[] firstLineArgs = parseRequest();
            String method = firstLineArgs[0];
            String requestedFile = firstLineArgs[1];
            String protocol = firstLineArgs[2];
            File file = new File("./" + requestedFile);

            if (!method.equals("GET")) {
                writeCannedResponse(protocol, 405, "Method not allowed");
            } else if (requestedFile.equals("/coffee")) {
                writeCannedResponse(protocol, 418, "I’m a teapot");
            } else if (requestedFile.equals("/tea/coffee")) {
                writeCannedResponse(
                        protocol,
                        503,
                        "Coffee is temporarily unavailable");
            } else if (file.exists()) {
                write200Response(protocol, loadFile(file), requestedFile);
            } else {
                write404Response(protocol, requestedFile);
            }
        } catch (IOException e) {
            System.out.println("There was an error parsing an HTTP header: " + e.getMessage());
        }

    }// processRequest method

    /*
     * Read the HTTP request from the input stream line by line up to
     * and including the empty line between the header and the
     * body. Send to the console every line read (except the last,
     * empty line). Then extract from the first line the HTTP command,
     * the path to the requested file, and the protocol description string and
     * return these three strings in an array.
     **/
    String[] parseRequest() throws IOException {
        StringBuilder output = new StringBuilder("\n*** request ***\n");
        String indent = "     ";

        String line = in.readLine();
        String firstLine = line;
        while (!line.isEmpty()) {
            output.append(indent).append(line).append("\n");
            line = in.readLine();
        }
        System.out.println(output);
        String[] tokens = firstLine.split("\\s");
        if (tokens.length == 3) {
            return tokens;
        } else {
            throw new IllegalArgumentException("HTTP header's first line was malformed");
        }

        return null; // only here to please the compiler
    }// parseRequest method

    /*
     * Given a File object for a file that we know is stored on the
     * server, return the contents of the file as a byte array
     **/
    byte[] loadFile(File file)
    {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            System.out.println(
                    "There was an error reading the file: " +
                            file.getName() + "\n" + e.getMessage()
            );
    }// loadFile method

    /*
     * Given an HTTP protocol description string, a byte array, and a file
     * name, send back to the client a 200 HTTP response whose body is the
     * input byte array. The file name is used to determine the type of
     * Web resource that is being returned. The set of required header
     * fields and file types is spelled out in the A3 handout.
     **/
    void write200Response(String protocol, byte[] body, String pathToFile) {
        try {
            String indent = "     ";
            String output = "*** response ***\n";
            output += indent + protocol + " 200 " + "Document Follows\n";
            output += indent + "Content-Length: " + body.length + "\n\n";
            output += indent + "<file contents not shown>\n";

            String response = protocol + " 200 " + "Document Follows\n" +
                    "Content-Length: " + body.length + "\n\n";
            byte[] payload = loadFile(new File("./" + pathToFile));

            System.out.println(output);
            out.writeUTF(response);
            out.write(payload);
        } catch (IOException e) {
            System.out.println(
                    "There was an error writing to the client: " + e.getMessage());
        }

    }// write200Response method

    /*
     * Given an HTTP protocol description string and a path that does not refer
     * to any of the existing files on the server, return to the client a 404
     * HTTP response whose body is a dynamically created page whose content
     * is spelled out in the A3 handout. The only HTTP header to be included
     * in the response is "Content-Type".
     **/
    void write404Response(String protocol, String pathToFile) {
        try {
            String indent = "     ";
            String output = "*** response ***\n";
            output += indent + protocol + " 404 Not Found\n";
            output += indent + "Content-Type: text/html\n\n";
            output += indent +
                    "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Page not found</title>" +
                    "</head><body><h1>HTTP Error 404 Not Found</h1><h2>The file" +
                    "<span style=\"color: red\">" +
                    pathToFile +
                    "</span>does not exist on this server.</h2></html></body>\n";

            String response = protocol + " 404 Not Found\n" +
                    "Content-Type: text/html\n\n" +
                    "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Page not found</title>" +
                    "</head><body><h1>HTTP Error 404 Not Found</h1><h2>The file" +
                    "<span style=\"color: red\"> " +
                    pathToFile +
                    " </span>does not exist on this server.</h2></html></body>\n";

            System.out.println(output);
            out.writeUTF(response);
        } catch (IOException e) {
            System.out.println(
                    "There was an error writing to the client: " + e.getMessage());
        }

    }// write404Response method

    /*
     * Given an HTTP protocol description string, a byte array, and a file
     * name, send back to the client a 200 HTTP response whose body is the
     * input byte array. The file name is used to determine the type of
     * Web resource that is being returned. The only HTTP header to be included
     * in the response is "Content-Type".
     **/
    void writeCannedResponse(String protocol, int code, String description) {
        String filename = "./html/" + code + ".html";
        try {
            String indent = "     ";
            String output = "*** response ***\n";
            output += indent + protocol + " " + code + " " + description + "\n";
            output += indent + "Content-Type: text/html\n\n";
            output += indent + "<contents of html/" + code + ".html not shown>\n";

            String response = protocol + " " + code + " " + description + "\n" +
                    "Content-Type: text/html\n\n";
            byte[] payload = loadFile(new File(filename));

            System.out.println(output);
            out.writeUTF(response);
            out.write(payload);

        } catch (FileNotFoundException e) {
            System.out.println(
                    "The file: " + filename + " was not found\n" + e.getMessage());
        } catch (IOException e) {
            System.out.println(
                    "There was a problem writing to a client: " + e.getMessage());
        }
    }// writeCannedResponse method

    /*
     * open the necessary I/O streams and initialize the in and out
     * variables; this method does not catch any IO exceptions.
     **/
    void openStreams(Socket clientSocket) throws IOException {
        in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        out = new DataOutputStream(clientSocket.getOutputStream());

    }// openStreams method

    /*
     * close all open I/O streams and sockets; also update and display the
     * number of connected clients.
     **/
    void close() {

        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }

            numConnections--;
            System.out.println(
                    "%% Connection released: " + clientSocket + "\n" +
                            "%% [# of connected clients: " + numConnections + "]");
        } catch (IOException e) {
            System.err.println("Error in close(): " + e.getMessage());
        }

    }// close method

}// WebServer class

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*  This class implements a simple client that connects to the server
 *  i.e illustrates how establish a connection to a server program using the Socket class
 *  and then, how the client can send data to and receive data from the server through the socket.
 */

/**
 *
 * @author annaf
 */

import java.io.*;
import java.net.*;

public class SimpleClient {
    public static void main(String[] args) throws IOException {

        Socket socket = null;       //Socket object for communicating
        PrintWriter out = null;    //socket output to server - for sending data through the socket to the server
        BufferedReader in = null;  //socket input from server - for reading server's response

        try {
            socket = new Socket("10.0.0.13", 45000);   //establish the socket connection between the client and the server
            out = new PrintWriter(socket.getOutputStream(), true);  //open a PrintWriter on the socket
            in = new BufferedReader(new InputStreamReader(
                                        socket.getInputStream()));  //open a BufferedReader on the socket
        
            System.out.print("Enter your message, to end write bye ");
            System.out.println();
            BufferedReader br = new BufferedReader(
                                       new InputStreamReader(System.in)); //for reading user input (line at a time from the standard input stream)
            String userInput;

            while ((userInput = br.readLine()) != null) {
                out.println(userInput);     //sends to the server immediately
                System.out.println("Your message: " + in.readLine());  // waits until the server sends back data,reads and prints it
                  if (userInput.equals("bye"))
                       break;
            }
            
            System.out.println("CLIENT: I LEAVED");
            
            //closing any streams connected to a socket  before closing this socket
            out.close();
            in.close();
            br.close();
            socket.close();
            
        } catch (UnknownHostException e) {
                System.out.println("Don't know about this host\n" + e.getMessage());            
                System.exit(1);
        } catch (IOException e) {
                System.out.println("Couldn't get I/O for "
                                   + "the connection to this host\n" + e.getMessage());
                System.exit(1);
        }
        
    }
}

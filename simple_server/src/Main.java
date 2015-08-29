/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author annaf
 */
//import java.net.*;
import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        
        //ServerJFrame serverJFrame = new ServerJFrame();
        
        //serverJFrame.setVisible(true);
        
        SimpleServer server = new SimpleServer(45000);
        new Thread(server).start();
        System.out.println("Server is waiting to connect");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
       //System.out.println("Stopping Server");
       //server.stop();

    }

}

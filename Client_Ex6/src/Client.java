
import java.net.*;
import java.io.*;
import java.util.*;

public class Client {

	// for I/O
	private ObjectInputStream sInput;		// to read from the socket
	private ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;

	// the server, the port and the username
	private String server, username;
	private int port;

	/*
	 *  Constructor called by console mode
	 */
	Client(String server, int port, String username) {
                this.server = server;
                this.port = port;
                this.username = username;
	}

	/*
	 * To start the dialog
	 */
	public boolean start() {
		// try to connect to the server
		try {
                        socket = new Socket(server, port);
		} 
		catch(Exception ec) {
                        display("Error connectiong to server:" + ec);
                        return false;
		}

		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);

		/* Creating both Data Stream */
		try
		{
                        sInput  = new ObjectInputStream(socket.getInputStream());
                        sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
                        display("Exception creating new Input/output Streams: " + eIO);
                        return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();
		// Send our username to the server this is the only message that we
		// will send as a String. All other messages will be ChatMessage objects
		try
		{
                        sOutput.writeObject(username);
		}
		catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}

	/*
	 * To send a message to the console or the GUI
	 */
	private void display(String msg) {
		System.out.println(msg);      
		clientGUI.BigTextArea.append(msg + "\n");
	}

	/*
	 * To send a message to the server
	 */
	void sendMessage(ChatMessage msg) {
		try {
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	/*
	 * When something goes wrong
	 * Close the Input/Output streams and disconnect
	 */
	private void disconnect() {
		try { 
			if(sInput != null) sInput.close();
		}
		catch(Exception e) {} 
		try {
			if(sOutput != null) sOutput.close();
		}
		catch(Exception e) {} 
		try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {} 

	}
	
	class ListenFromServer extends Thread {
            
		public void run() {
			
                        while(true) {
				try {
					String msg = (String) sInput.readObject();
					clientGUI.BigTextArea.append(msg);
				}
				catch(IOException e) {
					display("Server has close the connection: " + e);
					break;
				}
				catch(ClassNotFoundException e2) {}
			}
		}
	}
}


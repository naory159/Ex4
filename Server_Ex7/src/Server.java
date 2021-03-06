import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server extends Thread {
	// a unique ID for each connection
	private static int uniqueId;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	// to display time
	private SimpleDateFormat sdf;
	// the port number to listen for connection
	private int port;
	// the boolean that will be turned of to stop the server
	private boolean keepGoing;
        
        private int ftpConnections;
        
        ServerSocket newServerSocketForSend = null;
        
        public boolean [] serverPortList = {true, false, false, false, false, false, false, false, false, false};

	/*
	 *  server constructor that receive the port to listen to for connection as parameter
	 *  in console
	 */
	public Server(int port) {
		// the port
		this.port = port;
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss");
		// ArrayList for the Client list
		al = new ArrayList<ClientThread>();
                
                this.ftpConnections = 0;
	}

	public void run() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);
                        
			// infinite loop to wait for connections
			while(keepGoing) 
			{
				// format message saying we are waiting
				display("Server waiting for Clients on port " + port + ".");
                                
				Socket socket = serverSocket.accept();  // accept connection
				// if I was asked to stop
				if(!keepGoing) break;
				ClientThread t = new ClientThread(socket);  // make a thread of it
				al.add(t);                                  // save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					}
					catch(IOException ioE) {}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		// something went bad
		catch (IOException e) {
			String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}		

	protected void stopThread() {
		keepGoing = false;
		try {
			new Socket("localhost", port);
		}
		catch(Exception e) {}
	}
	
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
		serverGUI.serverTextArea.append(time + "\n");
	}
        
	/*
	 *  to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String message) {
		// add HH:mm:ss and \n to the message
		String time = sdf.format(new Date());
		String messageLf = time + " " + message + "\n";
		System.out.print(messageLf);
		serverGUI.serverTextArea.append(messageLf);     

		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(messageLf)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
	}
         
        /*
	 *  to broadcast a private message to all Clients
	 */
        private synchronized void broadcastOnlyOne(String message, ChatMessage cm) {
                
                // add HH:mm:ss and \n to the message
		String time = sdf.format(new Date());
		String messageLf = time + " " + message + "\r\n";
		System.out.print(messageLf);
		serverGUI.serverTextArea.append(messageLf);
                
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
                        if (cm.getType() == ChatMessage.ESTABLISHCONNECTION || cm.getType() == ChatMessage.ESTABLISHCONNECTIONANDPLAY) {
                            if(cm.getTO().equalsIgnoreCase(ct.username) && !ct.writeMsg(cm)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
                                break;
                            }
                        } else {
                            if(cm.getTO().equalsIgnoreCase(ct.username) && !ct.writeMsg(message)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
                                break;
                            }
                        }
		}
	}

	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// found it
			if(ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}

	/*
	 *  To run as a console application just open a console window and: 
	 * > java Server
	 * > java Server portNumber
	 * If the port number is not specified 1500 is used
	 */ 
	/*public static void main(String[] args) {
		// start server on port 1500 unless a PortNumber is specified 
		int portNumber = 1500;
		switch(args.length) {
		case 1:
			try {
				portNumber = Integer.parseInt(args[0]);
			}
			catch(Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java Server [portNumber]");
				return;
			}
		case 0:
			break;
		default:
			System.out.println("Usage is: > java Server [portNumber]");
			return;

		}
		// create a server object and start it
		Server server = new Server(portNumber);
		server.start();
	}*/

	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
                
                // the socket where to send file
		Socket sendSocket;
		ObjectInputStream sendSocketInput;
		ObjectOutputStream sendSocketOutput;
                
                // the socket where to send file
		Socket sendMessageSocket;
		ObjectInputStream endMessageSocketInput;
		ObjectOutputStream endMessageSocketOutput;
                
                Thread sendThread = null;
                
		// my unique id (easier for deconnection)
		int id;
		// the Username of the Client
		String username;
		// the only type of message a will receive
		ChatMessage cm;
		// the date I connect
		String date;

		// Constructor
		ClientThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) sInput.readObject();
				display(username + " just connected.");
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			// have to catch ClassNotFoundException
			// but I read a String, I am sure it will work
			catch (ClassNotFoundException e) {
			}
			date = new Date().toString() + "\n";
		}

		// what will run forever
		public void run() {
			// to loop until LOGOUT
			boolean keepGoing = true;
			while(keepGoing) {
				// read a String (which is an object)
				try {
					cm = (ChatMessage) sInput.readObject();
				}
				catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// the messaage part of the ChatMessage
				String message = cm.getMessage();

				// Switch on the type of message receive
				switch(cm.getType()) {

				case ChatMessage.MESSAGE:
					broadcast(username + ": " + message);
					break;
				case ChatMessage.LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;
				case ChatMessage.WHOISIN:
					writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
					// scan al the users connected
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
					}
					break;
                                case ChatMessage.TO:
                                    broadcastOnlyOne(username + ": " + message, cm);
                                    break;
                                case ChatMessage.SENDFILE:
                                    sendFile(username + ": " + message, cm);
                                    break;
                                case ChatMessage.PROCEED:
                                    proceedSendFile();
                                    break;
                                case ChatMessage.SENDMEDIAFILE:
                                    String fileList = getFileList();
                                    writeMsg(new ChatMessage(ChatMessage.SENDMEDIAFILE, fileList));
                                    break;
                                case ChatMessage.ESTABLISHCONNECTIONANDPLAY:
                                    sendMediaFile(username + ": " + message, cm);
                                    break;
                                }
                                
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients
			remove(id);
			close();
		}
                
                private String getFileList() {
                    File folder = new File(".");
                    File[] listOfFiles = folder.listFiles();
                    
                    String ans = "\n===========File Names===========\n";
                    
                    for (int i = 0; i < listOfFiles.length; i++) {
                        if (listOfFiles[i].isFile()) {
                            System.out.println("File " + listOfFiles[i].getName());
                            ans = ans + listOfFiles[i].getName() + "\n";
                        }
                    }
                    ans = ans + "========End Of File Names=======\n\n";
                    return ans;
                }
                
                private void proceedSendFile() {
                    synchronized(sendThread){
                        display("pushed proceed !");
                        sendThread.notify();
                    }
                }
                
                private void sendFile(final String message, final ChatMessage cm) {
                    sendThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            ftpConnections++;
                            
                            if (ftpConnections > 2) { 
                                ftpConnections--;
                                writeMsg(new ChatMessage(ChatMessage.MESSAGE, "Too many FTP connections! try again later" + "\n", username));
                                return;
                            }
                            
                            File f = new File(cm.getFileName());
                            long sizeOfFile = f.length();
                            FileInputStream fileInput = null;
                            try {
                                fileInput = new FileInputStream(f);
                                BufferedInputStream bufferInput = new BufferedInputStream(fileInput);
                                int i = findPortEmpty(serverPortList);
                                int newPort = 55000 + i;
                                
                                serverPortList[i] = true;
                                
                                newServerSocketForSend = null;
                                newServerSocketForSend = new ServerSocket(newPort);
                                
                                Socket sendSocket = new Socket();
                                
                                ChatMessage cmConnectToSendFile = new ChatMessage(ChatMessage.ESTABLISHCONNECTION, newPort + "", cm.getTO(), f.toString());
                                broadcastOnlyOne(username + ": " + message, cmConnectToSendFile);
                                
                                sendSocket = newServerSocketForSend.accept();
                                
                                BufferedOutputStream bufferedOutput = new BufferedOutputStream(sendSocket.getOutputStream());
                                int count;
                                int countTotal=0;
                                byte by[] = new byte[1];
                                while((count = bufferInput.read(by)) > 0){
                                    countTotal++;
                                    bufferedOutput.write(by);
                                    if(countTotal == sizeOfFile/2) {
                                        broadcastOnlyOne("Server: the 50% sent with the last bit: "+ Character.toString((char) by[0]) + " press procced to Continue" + "\n", cm);
                                        synchronized(sendThread) {
                                            sendThread.wait();
                                        }
                                    }
                                }
                                broadcastOnlyOne("Server: you downloaded 100% out of file" + "\n", cm);
                                broadcastOnlyOne("Server: " + cm.getTO() + " " + sendSocket.getInetAddress() + " " + sendSocket.getLocalPort() + " is now disconnected the FTP connection!" + "\n", cm);
                                bufferedOutput.close();
                                sendSocket.close();
                                newServerSocketForSend.close();
                                bufferInput.close();
                                
                                ftpConnections--;
                                
                                serverPortList[i] = false;
                                
                            } catch (FileNotFoundException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                writeMsg(new ChatMessage(ChatMessage.MESSAGE, "לשרת אין אפשרות לאתר את הקובץ המבוקERROR: ש!" + "\n", username));
                                ftpConnections--;
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                writeMsg(new ChatMessage(ChatMessage.MESSAGE, "ERROR" + "\n", username));
                                ftpConnections--;
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                writeMsg(new ChatMessage(ChatMessage.MESSAGE, "ERROR" + "\n", username));
                                ftpConnections--;
                            }
                        }
                    });
                    
                    sendThread.start();
                }
                
                private void sendMediaFile(final String message, final ChatMessage cm) {
                    sendThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            ftpConnections++;
                            
                            if (ftpConnections > 2) { 
                                ftpConnections--;
                                writeMsg(new ChatMessage(ChatMessage.MESSAGE, "Too many FTP connections! try again later" + "\n", username));
                                return;
                            }
                            
                            File f = new File(cm.getFileName());
                            long sizeOfFile = f.length();
                            FileInputStream fileInput = null;
                            try {
                                fileInput = new FileInputStream(f);
                                BufferedInputStream bufferInput = new BufferedInputStream(fileInput);
                                int i = findPortEmpty(serverPortList);
                                int newPort = 55000 + i;
                                
                                serverPortList[i] = true;
                                
                                newServerSocketForSend = null;
                                newServerSocketForSend = new ServerSocket(newPort);
                                
                                Socket sendSocket = new Socket();
                                
                                ChatMessage cmConnectToSendFile = new ChatMessage(ChatMessage.ESTABLISHCONNECTIONANDPLAY, newPort + "", cm.getTO(), f.toString());
                                broadcastOnlyOne(username + ": " + message, cmConnectToSendFile);
                                
                                sendSocket = newServerSocketForSend.accept();
                                
                                BufferedOutputStream bufferedOutput = new BufferedOutputStream(sendSocket.getOutputStream());
                                int count;
                                int countTotal=0;
                                byte by[] = new byte[1];
                                while((count = bufferInput.read(by)) > 0){
                                    countTotal++;
                                    bufferedOutput.write(by);
                                }
                                broadcastOnlyOne("Server: you downloaded 100% out of file" + "\n", cm);
                                broadcastOnlyOne("Server: " + cm.getTO() + " " + sendSocket.getInetAddress() + " " + sendSocket.getLocalPort() + " is now disconnected the FTP connection!" + "\n", cm);
                                bufferedOutput.close();
                                sendSocket.close();
                                newServerSocketForSend.close();
                                bufferInput.close();
                                
                                ftpConnections--;
                                
                                serverPortList[i] = false;
                                
                            } catch (FileNotFoundException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                writeMsg(new ChatMessage(ChatMessage.MESSAGE, "לשרת אין אפשרות לאתר את הקובץ המבוקERROR: ש!" + "\n", username));
                                ftpConnections--;
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                writeMsg(new ChatMessage(ChatMessage.MESSAGE, "ERROR" + "\n", username));
                                ftpConnections--;
                            }
                        }
                    });
                    
                    sendThread.start();
                }
                
                /*
                Find empty port to send file
                */
                public int findPortEmpty (boolean[] booleanArray) {
                    int ans = -1;
                    for (int i = 0 ; i < booleanArray.length ; i++) {
                        if (booleanArray[i]==false) {
                            return i;
                        }
                    }
                    return ans;
                }
                
		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
                
                /*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(ChatMessage cm) {
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
                                boolean sendChatMessage = cm.getType() == ChatMessage.ESTABLISHCONNECTION;
                                boolean sendMediaFileMessage = cm.getType() == ChatMessage.ESTABLISHCONNECTIONANDPLAY;
                                if (sendChatMessage || sendMediaFileMessage)
                                    sOutput.writeObject(cm);
                                else
                                    sOutput.writeObject(cm.getMessage());
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}
}




import java.awt.BorderLayout;
import java.awt.Component;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

import javax.media.Manager;
import javax.media.Player;
import javax.media.CannotRealizeException;
import javax.media.NoPlayerException;
import javax.swing.JPanel;

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
        
        public void checkMessage(final ChatMessage cm) {
            if (cm.getType() == ChatMessage.ESTABLISHCONNECTION) {
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            byte[] aByte = new byte[1];
                            int bytesRead;
                            
                            int port = Integer.parseInt(cm.getMessage());
                            
                            Socket clientSocket = new Socket(socket.getInetAddress(), port);
                            
                            BufferedInputStream in = new BufferedInputStream(clientSocket.getInputStream());
                            String filename = clientGUI.fileNameToSend.getText();
                            File f = new File(filename);
                            FileOutputStream f1 = new FileOutputStream(f);
                            BufferedOutputStream bos = new BufferedOutputStream(f1);

                            while(in.read(aByte)!=-1){
                                f1.write(aByte);
                            }
                            f1.close();
                            clientSocket.close();
                            
                        } catch (IOException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                    }
                });
                
                t.start();
            } else if (cm.getType() == ChatMessage.SENDMEDIAFILE) {
                clientGUI.BigTextArea.append(cm.getMessage() + "\n");
            } else if (cm.getType() == ChatMessage.ESTABLISHCONNECTIONANDPLAY) {
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            byte[] aByte = new byte[1];
                            int bytesRead;
                            
                            int port = Integer.parseInt(cm.getMessage());
                            
                            Socket clientSocket = new Socket(socket.getInetAddress(), port);
                            
                            BufferedInputStream in = new BufferedInputStream(clientSocket.getInputStream());
                            String filename = clientGUI.mediaFileNameToSend.getText();
                            File f = new File(filename);
                            FileOutputStream f1 = new FileOutputStream(f);
                            BufferedOutputStream bos = new BufferedOutputStream(f1);

                            while(in.read(aByte)!=-1){
                                f1.write(aByte);
                            }
                            f1.close();
                            clientSocket.close();
                            
                            display("Media file download succesful!");
                            display("Please wait! the media file will start in a momment!");
                            
                            final URL mediaUrl = f.toURI().toURL();
                                                        
                            if ( mediaUrl != null ) { // only display if there is a valid URL
                                
                                new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        JFrame mediaPlayer = new JFrame( "Simple Media Player" );

                                        mediaPlayer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                                        MediaPlayer mediaPanel = new MediaPlayer(mediaUrl);

                                        mediaPlayer.add(mediaPanel);

                                        mediaPlayer.setSize(400, 400); // set the size of the player

                                        mediaPlayer.setVisible(true);
                                    }
                                }).start();
                            }
                            
                        } catch (IOException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                    }
                });
                
                t.start();
            }
        }
	
	class ListenFromServer extends Thread {
            
		public void run() {
			
                        while(true) {
				try {
                                    Object fromServer = sInput.readObject();
                                    
                                    if (fromServer instanceof String) {
                                        clientGUI.BigTextArea.append((String)fromServer);
                                        
                                        // GUI changes
                                        if (((String)fromServer).contains("press procced to Continue"))
                                            clientGUI.ProceedButton.setEnabled(true);
                                        else if (  ((String)fromServer).contains("Server: you downloaded 100% out of file")
                                                || ((String)fromServer).contains("Too many FTP connections")
                                                || ((String)fromServer).contains("ERROR"))
                                            clientGUI.getFileButton.setEnabled(true);
                                    } else if (fromServer instanceof ChatMessage) {
                                        ChatMessage cm = (ChatMessage)fromServer;
                                        checkMessage(cm);
                                    }
				}
				catch(IOException e) {
					display("Server has close the connection: " + e);
					break;
				}
				catch(ClassNotFoundException e2) {}
			}
		}
	}
        
        public class MediaPlayer extends JPanel {

            //Constructor
            public MediaPlayer(URL mediauUrl) {        

                setLayout(new BorderLayout());  //use a BorderLayout

                // Use lightweight components for Swing compatibility
                Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, true);

                try {
                    // create a player to play the media specified in the URL
                    Player mediaPlayer = Manager.createRealizedPlayer( mediauUrl );

                    // get the components for the video and the playback controls
                    Component video = mediaPlayer.getVisualComponent();
                    Component control = mediaPlayer.getControlPanelComponent();

                    if (video != null) {
                        add(video, BorderLayout.CENTER);          // place the video component in the panel
                    }

                    if (control != null) {
                        add(control, BorderLayout.SOUTH);         // place the control component in the panel
                    }

                    mediaPlayer.start(); // start playing the media clip

                } catch (NoPlayerException noPlayerException) {
                    System.err.println("No media player found: " + noPlayerException);
                } catch (CannotRealizeException cannotRealizeException ) {
                    System.err.println( "Could not realize media player: " + cannotRealizeException );
                } catch (IOException iOException) {
                    System.err.println( "Error reading from the source: " + iOException);
                }

            }
        }
}

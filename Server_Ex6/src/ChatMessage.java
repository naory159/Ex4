
import java.io.*;

// ======================================================
// === See Notes int the Client.ChatMessage.java file ===
// ======================================================
public class ChatMessage implements Serializable {

	
	static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, TO = 3, SENDFILE = 4, ESTABLISHCONNECTION = 5, PROCEED = 6;
	private int type;
	private String message;
        private String to;
        private String fileName;
	
	ChatMessage(int type, String message) {
		this.type = type;
		this.message = message;
	}
        
        ChatMessage(int type, String message, String to) {
		this.type = type;
		this.message = message;
                this.to = to;
	}

	ChatMessage(int type, String message, String to, String fileName) {
		this.type = type;
		this.message = message;
                this.to = to;
                this.fileName = fileName;
	}
        
	int getType() {
		return type;
	}
	String getMessage() {
		return message;
	}
        
        String getTO() {
            return to;
        }
        
        String getFileName() {
            return fileName;
        }
}


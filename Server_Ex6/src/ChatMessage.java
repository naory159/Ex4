
import java.io.*;

// ======================================================
// === See Notes int the Client.ChatMessage.java file ===
// ======================================================
public class ChatMessage implements Serializable {

	
	static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, TO = 3;
	private int type;
	private String message;
        private String to;
	
	ChatMessage(int type, String message) {
		this.type = type;
		this.message = message;
	}
        
        ChatMessage(int type, String message, String to) {
		this.type = type;
		this.message = message;
                this.to = to;
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
        
}


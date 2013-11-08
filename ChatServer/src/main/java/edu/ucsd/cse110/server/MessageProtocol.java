package edu.ucsd.cse110.server;

public class MessageProtocol {
    public String handleProtocolMessage(String messageText) {
        String responseText;
        //if ("MyProtocolMessage".equalsIgnoreCase(messageText)) {
        //    responseText = "I recognize your protocol message";
        //} else {
            responseText = messageText;
        //}
         
        return responseText;
    }
}
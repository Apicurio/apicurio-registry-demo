package io.apicurio.registry.demo.simple.avro;

import java.util.Date;

public class Greeting {

    private String message;
    private Date timestamp;
    
    public Greeting() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Greetings: " + message + " (" + timestamp + ")";
    }
    
}

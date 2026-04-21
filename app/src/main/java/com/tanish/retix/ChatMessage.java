package com.tanish.retix;

public class ChatMessage {
    private String text;
    private boolean isBuyer; // true = buyer (right side), false = seller (left side)
    private String time;

    public ChatMessage(String text, boolean isBuyer, String time) {
        this.text = text;
        this.isBuyer = isBuyer;
        this.time = time;
    }

    public String getText() { return text; }
    public boolean isBuyer() { return isBuyer; }
    public String getTime() { return time; }
}

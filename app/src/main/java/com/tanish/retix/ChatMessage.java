package com.tanish.retix;

public class ChatMessage {
    private String text;
    private boolean isBuyer; // true = buyer (right side), false = seller (left side)
    private String time;
    private long timestamp; // used for deduplication

    public ChatMessage(String text, boolean isBuyer, String time) {
        this.text      = text;
        this.isBuyer   = isBuyer;
        this.time      = time;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String text, boolean isBuyer, String time, long timestamp) {
        this.text      = text;
        this.isBuyer   = isBuyer;
        this.time      = time;
        this.timestamp = timestamp;
    }

    public String  getText()      { return text; }
    public boolean isBuyer()      { return isBuyer; }
    public String  getTime()      { return time; }
    public long    getTimestamp() { return timestamp; }

    // Equality based on content + timestamp so duplicates can be detected
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessage)) return false;
        ChatMessage other = (ChatMessage) o;
        return timestamp == other.timestamp
                && isBuyer == other.isBuyer
                && java.util.Objects.equals(text, other.text);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(text, isBuyer, timestamp);
    }
}

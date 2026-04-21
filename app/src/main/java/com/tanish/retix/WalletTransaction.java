package com.tanish.retix;

public class WalletTransaction {
    private String eventName;
    private String date;
    private int price;
    private int status; // 0 = Processing, 1 = Available
    private boolean isPurchase; // true = purchase, false = sale

    public static final int STATUS_PROCESSING = 0;
    public static final int STATUS_AVAILABLE = 1;

    public WalletTransaction(String eventName, String date, int price, int status, boolean isPurchase) {
        this.eventName = eventName;
        this.date = date;
        this.price = price;
        this.status = status;
        this.isPurchase = isPurchase;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isProcessing() {
        return status == STATUS_PROCESSING;
    }

    public boolean isAvailable() {
        return status == STATUS_AVAILABLE;
    }

    public boolean isPurchase() {
        return isPurchase;
    }

    public void setPurchase(boolean purchase) {
        isPurchase = purchase;
    }

    public boolean isSale() {
        return !isPurchase;
    }

    public String getStatusText() {
        return status == STATUS_PROCESSING ? "Processing" : "Available";
    }
}

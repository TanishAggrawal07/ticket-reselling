package com.tanish.retix;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Ticket implements Parcelable {
    private String eventName;
    private String date;
    private int originalPrice;
    private int sellingPrice;
    private String sellerName;
    private float rating;

    // New fields for image and file support
    private int eventImageResId;   // drawable resource id for dummy event image
    private String eventImageUri;  // URI string for user-picked image
    private String ticketFileUri;  // URI string for uploaded ticket PDF/image

    public Ticket(String eventName, String date, int originalPrice, int sellingPrice,
                  String sellerName, float rating) {
        this.eventName = eventName;
        this.date = date;
        this.originalPrice = originalPrice;
        this.sellingPrice = sellingPrice;
        this.sellerName = sellerName;
        this.rating = rating;
        this.eventImageResId = 0;
        this.eventImageUri = null;
        this.ticketFileUri = null;
    }

    public Ticket(String eventName, String date, int originalPrice, int sellingPrice,
                  String sellerName, float rating, int eventImageResId) {
        this(eventName, date, originalPrice, sellingPrice, sellerName, rating);
        this.eventImageResId = eventImageResId;
    }

    protected Ticket(Parcel in) {
        eventName = in.readString();
        date = in.readString();
        originalPrice = in.readInt();
        sellingPrice = in.readInt();
        sellerName = in.readString();
        rating = in.readFloat();
        eventImageResId = in.readInt();
        eventImageUri = in.readString();
        ticketFileUri = in.readString();
    }

    public static final Creator<Ticket> CREATOR = new Creator<Ticket>() {
        @Override
        public Ticket createFromParcel(Parcel in) {
            return new Ticket(in);
        }

        @Override
        public Ticket[] newArray(int size) {
            return new Ticket[size];
        }
    };

    // --- Getters & Setters ---

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(int originalPrice) { this.originalPrice = originalPrice; }

    public int getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(int sellingPrice) { this.sellingPrice = sellingPrice; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getEventImageResId() { return eventImageResId; }
    public void setEventImageResId(int eventImageResId) { this.eventImageResId = eventImageResId; }

    public String getEventImageUri() { return eventImageUri; }
    public void setEventImageUri(String eventImageUri) { this.eventImageUri = eventImageUri; }

    public boolean hasEventImage() {
        return eventImageResId != 0 || (eventImageUri != null && !eventImageUri.isEmpty());
    }

    public String getTicketFileUri() { return ticketFileUri; }
    public void setTicketFileUri(String ticketFileUri) { this.ticketFileUri = ticketFileUri; }

    public boolean hasTicketFile() {
        return ticketFileUri != null && !ticketFileUri.isEmpty();
    }

    /**
     * Returns the best drawable resource ID to display on the ticket card.
     *
     * Priority:
     *   1. Explicitly set eventImageResId (dummy data or seller-assigned drawable)
     *   2. Keyword match on the event name → category-specific gradient
     *   3. Generic default_event_image as the final fallback
     *
     * This ensures every card always shows a visually appropriate image.
     */
    public int getSmartImageResId() {
        // 1. Explicit drawable already assigned
        if (eventImageResId != 0) return eventImageResId;

        // 2. Keyword-based category detection
        if (eventName != null) {
            String lower = eventName.toLowerCase();

            // Sports — cricket, football, IPL, FIFA, NBA, tennis, kabaddi, etc.
            if (lower.contains("ipl") || lower.contains("cricket")
                    || lower.contains("football") || lower.contains("soccer")
                    || lower.contains("nba") || lower.contains("tennis")
                    || lower.contains("badminton") || lower.contains("kabaddi")
                    || lower.contains("match") || lower.contains("league")
                    || lower.contains("championship") || lower.contains("tournament")
                    || lower.contains("grand prix") || lower.contains("f1")
                    || lower.contains("sport") || lower.contains("stadium")
                    || lower.contains("rcb") || lower.contains("csk")
                    || lower.contains("mi ") || lower.contains("kkr")) {
                return R.drawable.bg_event_sports;
            }

            // Festival / EDM / rave / sunburn / tomorrowland
            if (lower.contains("festival") || lower.contains("sunburn")
                    || lower.contains("tomorrowland") || lower.contains("edm")
                    || lower.contains("rave") || lower.contains("vh1")
                    || lower.contains("lollapalooza") || lower.contains("coachella")
                    || lower.contains("nh7") || lower.contains("weekender")) {
                return R.drawable.bg_event_festival;
            }

            // Stand-up comedy / comedy show / open mic
            if (lower.contains("comedy") || lower.contains("stand-up")
                    || lower.contains("standup") || lower.contains("open mic")
                    || lower.contains("laugh") || lower.contains("kapil")
                    || lower.contains("zakir") || lower.contains("biswa")
                    || lower.contains("kunal kamra") || lower.contains("roast")) {
                return R.drawable.bg_event_standup;
            }

            // Theatre / drama / play / musical
            if (lower.contains("theatre") || lower.contains("theater")
                    || lower.contains("drama") || lower.contains("play")
                    || lower.contains("musical") || lower.contains("opera")
                    || lower.contains("ballet") || lower.contains("broadway")
                    || lower.contains("mime") || lower.contains("puppet")) {
                return R.drawable.bg_event_theatre;
            }

            // Tech / conference / summit / hackathon / startup
            if (lower.contains("tech") || lower.contains("conference")
                    || lower.contains("summit") || lower.contains("hackathon")
                    || lower.contains("startup") || lower.contains("devfest")
                    || lower.contains("google i/o") || lower.contains("wwdc")
                    || lower.contains("workshop") || lower.contains("seminar")
                    || lower.contains("expo") || lower.contains("conclave")) {
                return R.drawable.bg_event_tech;
            }

            // Food / culinary / food festival / food fair
            if (lower.contains("food") || lower.contains("culinary")
                    || lower.contains("chef") || lower.contains("taste")
                    || lower.contains("eat") || lower.contains("dining")
                    || lower.contains("bbq") || lower.contains("barbeque")
                    || lower.contains("wine") || lower.contains("beer")) {
                return R.drawable.bg_event_food;
            }

            // Art / exhibition / gallery / museum
            if (lower.contains("art") || lower.contains("exhibition")
                    || lower.contains("gallery") || lower.contains("museum")
                    || lower.contains("exhibit") || lower.contains("show")
                    || lower.contains("fair") || lower.contains("expo")) {
                return R.drawable.bg_event_exhibition;
            }

            // Coldplay-specific (already has its own drawable)
            if (lower.contains("coldplay")) {
                return R.drawable.bg_event_coldplay;
            }

            // Generic music / concert / live / tour / gig
            if (lower.contains("concert") || lower.contains("live")
                    || lower.contains("tour") || lower.contains("gig")
                    || lower.contains("music") || lower.contains("band")
                    || lower.contains("singer") || lower.contains("arijit")
                    || lower.contains("ed sheeran") || lower.contains("dj")
                    || lower.contains("unplugged") || lower.contains("acoustic")) {
                return R.drawable.bg_event_concert;
            }
        }

        // 3. Final fallback — always looks good
        return R.drawable.default_event_image;
    }

    // --- Business Logic ---

    public int getSavings() { return originalPrice - sellingPrice; }

    public boolean isDiscounted() { return sellingPrice < originalPrice; }

    // --- Parcelable ---

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(eventName);
        dest.writeString(date);
        dest.writeInt(originalPrice);
        dest.writeInt(sellingPrice);
        dest.writeString(sellerName);
        dest.writeFloat(rating);
        dest.writeInt(eventImageResId);
        dest.writeString(eventImageUri);
        dest.writeString(ticketFileUri);
    }
}

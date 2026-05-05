package com.tanish.retix;

import android.os.Parcel;
import android.os.Parcelable;

public class Ticket implements Parcelable {

    // ── Core fields (Firestore + local) ───────────────────────────────────────
    private String firestoreId;     // Firestore document ID
    private String eventName;
    private String date;            // display string e.g. "Sat, Jan 18 • 7:00 PM"
    private String eventDate;       // ISO date string for sorting
    private int    originalPrice;
    private int    sellingPrice;
    private String sellerName;
    private String sellerId;        // Firebase Auth UID of the seller
    private float  rating;
    private String status;          // "available" | "sold"

    // ── Image / file fields ───────────────────────────────────────────────────
    private int    eventImageResId;  // local drawable fallback (dummy data)
    private String eventImageUri;    // local URI (picked from gallery, pre-upload)
    private String eventImageUrl;    // Firebase Storage download URL (after upload)
    private String ticketFileUri;    // local URI (picked from device, pre-upload)
    private String ticketFileUrl;    // Firebase Storage download URL (after upload)

    // ── Status constants ──────────────────────────────────────────────────────
    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_SOLD      = "sold";

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Minimal constructor — used for dummy/local data */
    public Ticket(String eventName, String date, int originalPrice, int sellingPrice,
                  String sellerName, float rating) {
        this.eventName      = eventName;
        this.date           = date;
        this.originalPrice  = originalPrice;
        this.sellingPrice   = sellingPrice;
        this.sellerName     = sellerName;
        this.rating         = rating;
        this.status         = STATUS_AVAILABLE;
        this.eventImageResId = 0;
    }

    /** Constructor with local drawable resource (dummy data with image) */
    public Ticket(String eventName, String date, int originalPrice, int sellingPrice,
                  String sellerName, float rating, int eventImageResId) {
        this(eventName, date, originalPrice, sellingPrice, sellerName, rating);
        this.eventImageResId = eventImageResId;
    }

    /** Full constructor — used when building from a Firestore document */
    public Ticket(String firestoreId, String eventName, String date, String eventDate,
                  int originalPrice, int sellingPrice,
                  String sellerName, String sellerId, float rating,
                  String status, String eventImageUrl, String ticketFileUrl) {
        this.firestoreId    = firestoreId;
        this.eventName      = eventName;
        this.date           = date;
        this.eventDate      = eventDate;
        this.originalPrice  = originalPrice;
        this.sellingPrice   = sellingPrice;
        this.sellerName     = sellerName;
        this.sellerId       = sellerId;
        this.rating         = rating;
        this.status         = status;
        this.eventImageUrl  = eventImageUrl;
        this.ticketFileUrl  = ticketFileUrl;
        this.eventImageResId = 0;
    }

    // ── Parcelable ────────────────────────────────────────────────────────────

    protected Ticket(Parcel in) {
        firestoreId      = in.readString();
        eventName        = in.readString();
        date             = in.readString();
        eventDate        = in.readString();
        originalPrice    = in.readInt();
        sellingPrice     = in.readInt();
        sellerName       = in.readString();
        sellerId         = in.readString();
        rating           = in.readFloat();
        status           = in.readString();
        eventImageResId  = in.readInt();
        eventImageUri    = in.readString();
        eventImageUrl    = in.readString();
        ticketFileUri    = in.readString();
        ticketFileUrl    = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(firestoreId);
        dest.writeString(eventName);
        dest.writeString(date);
        dest.writeString(eventDate);
        dest.writeInt(originalPrice);
        dest.writeInt(sellingPrice);
        dest.writeString(sellerName);
        dest.writeString(sellerId);
        dest.writeFloat(rating);
        dest.writeString(status);
        dest.writeInt(eventImageResId);
        dest.writeString(eventImageUri);
        dest.writeString(eventImageUrl);
        dest.writeString(ticketFileUri);
        dest.writeString(ticketFileUrl);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<Ticket> CREATOR = new Creator<Ticket>() {
        @Override public Ticket createFromParcel(Parcel in) { return new Ticket(in); }
        @Override public Ticket[] newArray(int size)        { return new Ticket[size]; }
    };

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getFirestoreId()                          { return firestoreId; }
    public void   setFirestoreId(String firestoreId)        { this.firestoreId = firestoreId; }

    public String getEventName()                            { return eventName; }
    public void   setEventName(String eventName)            { this.eventName = eventName; }

    public String getDate()                                 { return date; }
    public void   setDate(String date)                      { this.date = date; }

    public String getEventDate()                            { return eventDate; }
    public void   setEventDate(String eventDate)            { this.eventDate = eventDate; }

    public int    getOriginalPrice()                        { return originalPrice; }
    public void   setOriginalPrice(int originalPrice)       { this.originalPrice = originalPrice; }

    public int    getSellingPrice()                         { return sellingPrice; }
    public void   setSellingPrice(int sellingPrice)         { this.sellingPrice = sellingPrice; }

    public String getSellerName()                           { return sellerName; }
    public void   setSellerName(String sellerName)          { this.sellerName = sellerName; }

    public String getSellerId()                             { return sellerId; }
    public void   setSellerId(String sellerId)              { this.sellerId = sellerId; }

    public float  getRating()                               { return rating; }
    public void   setRating(float rating)                   { this.rating = rating; }

    public String getStatus()                               { return status; }
    public void   setStatus(String status)                  { this.status = status; }

    public int    getEventImageResId()                      { return eventImageResId; }
    public void   setEventImageResId(int eventImageResId)   { this.eventImageResId = eventImageResId; }

    public String getEventImageUri()                        { return eventImageUri; }
    public void   setEventImageUri(String eventImageUri)    { this.eventImageUri = eventImageUri; }

    public String getEventImageUrl()                        { return eventImageUrl; }
    public void   setEventImageUrl(String eventImageUrl)    { this.eventImageUrl = eventImageUrl; }

    public String getTicketFileUri()                        { return ticketFileUri; }
    public void   setTicketFileUri(String ticketFileUri)    { this.ticketFileUri = ticketFileUri; }

    public String getTicketFileUrl()                        { return ticketFileUrl; }
    public void   setTicketFileUrl(String ticketFileUrl)    { this.ticketFileUrl = ticketFileUrl; }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public boolean hasEventImage() {
        return eventImageResId != 0
                || (eventImageUri  != null && !eventImageUri.isEmpty())
                || (eventImageUrl  != null && !eventImageUrl.isEmpty());
    }

    public boolean hasTicketFile() {
        return (ticketFileUri != null && !ticketFileUri.isEmpty())
                || (ticketFileUrl != null && !ticketFileUrl.isEmpty());
    }

    /** Best ticket file reference: prefer the remote URL, fall back to local URI */
    public String getBestTicketFileRef() {
        if (ticketFileUrl != null && !ticketFileUrl.isEmpty()) return ticketFileUrl;
        return ticketFileUri;
    }

    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(status);
    }

    // ── Business logic ────────────────────────────────────────────────────────

    public int     getSavings()    { return originalPrice - sellingPrice; }
    public boolean isDiscounted()  { return sellingPrice < originalPrice; }

    // ── Smart default image (keyword-based) ───────────────────────────────────

    /**
     * Returns the best drawable resource ID for the ticket card banner.
     * Priority: explicit resId → keyword match → generic fallback.
     */
    public int getSmartImageResId() {
        if (eventImageResId != 0) return eventImageResId;

        if (eventName != null) {
            String lower = eventName.toLowerCase();

            if (lower.contains("ipl") || lower.contains("cricket")
                    || lower.contains("football") || lower.contains("soccer")
                    || lower.contains("nba") || lower.contains("tennis")
                    || lower.contains("badminton") || lower.contains("kabaddi")
                    || lower.contains("match") || lower.contains("league")
                    || lower.contains("championship") || lower.contains("tournament")
                    || lower.contains("grand prix") || lower.contains("f1")
                    || lower.contains("sport") || lower.contains("stadium")
                    || lower.contains("rcb") || lower.contains("csk")
                    || lower.contains("mi ") || lower.contains("kkr"))
                return R.drawable.bg_event_sports;

            if (lower.contains("festival") || lower.contains("sunburn")
                    || lower.contains("tomorrowland") || lower.contains("edm")
                    || lower.contains("rave") || lower.contains("vh1")
                    || lower.contains("lollapalooza") || lower.contains("coachella")
                    || lower.contains("nh7") || lower.contains("weekender"))
                return R.drawable.bg_event_festival;

            if (lower.contains("comedy") || lower.contains("stand-up")
                    || lower.contains("standup") || lower.contains("open mic")
                    || lower.contains("laugh") || lower.contains("kapil")
                    || lower.contains("zakir") || lower.contains("biswa")
                    || lower.contains("kunal kamra") || lower.contains("roast"))
                return R.drawable.bg_event_standup;

            if (lower.contains("theatre") || lower.contains("theater")
                    || lower.contains("drama") || lower.contains("play")
                    || lower.contains("musical") || lower.contains("opera")
                    || lower.contains("ballet") || lower.contains("broadway"))
                return R.drawable.bg_event_theatre;

            if (lower.contains("tech") || lower.contains("conference")
                    || lower.contains("summit") || lower.contains("hackathon")
                    || lower.contains("startup") || lower.contains("devfest")
                    || lower.contains("workshop") || lower.contains("seminar"))
                return R.drawable.bg_event_tech;

            if (lower.contains("food") || lower.contains("culinary")
                    || lower.contains("chef") || lower.contains("taste")
                    || lower.contains("eat") || lower.contains("dining"))
                return R.drawable.bg_event_food;

            if (lower.contains("art") || lower.contains("exhibition")
                    || lower.contains("gallery") || lower.contains("museum"))
                return R.drawable.bg_event_exhibition;

            if (lower.contains("coldplay"))
                return R.drawable.bg_event_coldplay;

            if (lower.contains("concert") || lower.contains("live")
                    || lower.contains("tour") || lower.contains("gig")
                    || lower.contains("music") || lower.contains("band")
                    || lower.contains("singer") || lower.contains("arijit")
                    || lower.contains("ed sheeran") || lower.contains("dj"))
                return R.drawable.bg_event_concert;
        }

        return R.drawable.default_event_image;
    }
}

# ReTix Android App Documentation

## Overview

ReTix is an Android ticket resale marketplace app targeting the Indian market (INR pricing). Users can list event tickets for sale, browse and purchase tickets from others, manage a virtual wallet, and chat with sellers. The app communicates with an Express.js/MySQL backend via Retrofit.

**Package:** `com.tanish.retix`
**Min SDK:** 24 (Android 7.0)
**Target SDK:** 36
**Language:** Java 11

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Activities                         │
│  Splash → Onboarding → Login/Signup → MainActivity      │
│         (BottomNav: Home | Tickets | Wallet | Profile)  │
│         + TicketDetail, Chat, SellerProfile, Settings,   │
│           EditProfile                                    │
└──────────────────────┬──────────────────────────────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
     ApiManager    TokenManager  SharedPreferences
     (singleton    (JWT + user    (dark mode,
      facade)       info cache)    profile edits)
          │
          ▼
      ApiClient (Retrofit + OkHttp + JWT interceptor)
          │
          ▼
      ApiService (Retrofit interface, ~30 endpoints)
          │
          ▼
   Express.js Backend (REST API)
```

### Design Patterns

- **Singleton facade** — `ApiManager` wraps all API calls; `ApiClient` manages the Retrofit instance
- **Token-based auth** — JWT stored in `SharedPreferences` via `TokenManager`; OkHttp interceptor auto-attaches `Authorization: Bearer <token>` header
- **Callback-based async** — All API calls use `ApiManager.Callback<T>` interface with `onSuccess(T)` and `onError(String)`
- **Fragment-based navigation** — `MainActivity` hosts 4 fragments via `BottomNavigationView`

---

## Build Configuration

**File:** `app/build.gradle.kts`

| Setting | Value |
|---------|-------|
| namespace | `com.tanish.retix` |
| compileSdk | 36 |
| minSdk | 24 |
| targetSdk | 36 |
| versionCode | 1 |
| versionName | 1.0 |
| Java compatibility | 11 |
| ViewBinding | Enabled |
| ProGuard | Disabled (isMinifyEnabled = false) |

### Dependencies

| Category | Library | Version |
|----------|---------|---------|
| AndroidX Core | appcompat | 1.7.0 |
| Material | material | 1.12.0 |
| Activity | activity | 1.9.3 |
| Layout | constraintlayout | 2.2.0 |
| ViewPager2 | viewpager2 | 1.1.0 |
| RecyclerView | recyclerview | 1.3.2 |
| Networking | Retrofit | 2.11.0 |
| JSON Converter | converter-gson | 2.11.0 |
| HTTP Client | OkHttp | 4.12.0 |
| HTTP Logging | logging-interceptor | 4.12.0 |
| Image Loading | Glide | 4.16.0 |
| Testing | JUnit | 4.13.2 |
| Android Test | androidx.test.ext | 1.2.1 |
| UI Test | Espresso | 3.6.1 |

> **Note:** Firebase dependencies have been fully removed. All data goes through the REST API.

---

## Screens and Navigation Flow

```
SplashActivity
  │
  ├─ (no token, first launch) → OnboardingActivity → LoginActivity
  ├─ (no token) → LoginActivity
  └─ (valid token) → MainActivity
                      │
                      ├── HomeFragment (browse tickets)
                      │     ├── TicketDetailActivity (view/buy)
                      │     │     ├── ChatActivity (message seller)
                      │     │     └── SellerProfileActivity (view seller)
                      │     └── SellFragment (list ticket)
                      │
                      ├── TicketsFragment (my purchases)
                      │     └── TicketDetailActivity
                      │
                      ├── WalletFragment (balance & history)
                      │
                      └── ProfileFragment (profile & stats)
                            ├── EditProfileActivity
                            └── SettingsActivity
                                  ├── Dark mode toggle
                                  ├── Edit profile link
                                  ├── Logout
                                  └── About dialog
```

### Activity Details

#### SplashActivity
- Entry point with 1.5s splash delay
- Checks dark mode preference from SharedPreferences
- Checks JWT token to route to Onboarding / Login / MainActivity

#### OnboardingActivity
- 3-slide ViewPager2 intro (ticket, lock, checkmark emojis)
- "Skip" or "Get Started" → LoginActivity

#### LoginActivity
- Email/password form with input validation
- Displays error messages for invalid credentials
- On success: saves JWT token + caches name/email → navigates to MainActivity

#### SignupActivity
- Name/email/password registration form
- Handles error codes: `EMAIL_EXISTS`, `WEAK_PASSWORD`
- On success: same flow as login

#### MainActivity
- Container with `BottomNavigationView` (4 tabs)
- Fragment switching with fade animation
- Handles ticket click events from HomeFragment → TicketDetailActivity
- Handles sell navigation → SellFragment

#### TicketDetailActivity
- Full ticket detail with image, pricing, seller info
- Buy button with wallet balance check and confirmation dialog
- Chat with seller button → ChatActivity
- View ticket file button (opens PDF/image in browser)
- Seller profile button → SellerProfileActivity
- Star rating dialog (UI-only, not persisted to API)
- "Your Listing" badge for own tickets
- "Purchased" state for bought tickets

#### ChatActivity
- Buyer-seller chat with polling every 3s
- Optimistic UI updates on send
- Self-chat prevention
- RecyclerView with left/right bubble layout

#### SellerProfileActivity
- Displays seller name, initial, rating
- Lists seller's available tickets

#### SettingsActivity
- Dark mode toggle (AppCompatDelegate, persisted in SharedPreferences)
- Edit profile link → EditProfileActivity
- Logout (clears JWT + login flag)
- About dialog

#### EditProfileActivity
- Edit name/email with validation
- Image picker for avatar
- Saves to SharedPreferences locally
- **Known limitation:** Profile changes are not pushed to the API

---

## Fragments

### HomeFragment
- Browse all available tickets via `ApiManager.fetchAvailableTickets()`
- RecyclerView with `TicketAdapter`
- Search/filter functionality
- Skeleton loading (ShimmerView)
- Empty state layout
- FAB to sell → SellFragment
- Settings button in toolbar

### SellFragment
- Create ticket listing form
- Fields: event name, date (DatePicker), original price, selling price
- Image picker for event image → uploads to Cloudinary via API
- Ticket file picker (PDF/image)
- Pricing recovery message
- Upload timeout handling

### WalletFragment
- Wallet balance display with polling every 5s
- Two sections: Purchases and Sales (RecyclerViews)
- Empty state for each section
- Pending balance display

### ProfileFragment
- User profile with skeleton loading
- Shows name, email, profile image (Glide)
- Stats cards: Sold, Earned, Active listings
- SalesBarChartView for monthly earnings
- Edit profile / logout buttons
- Image picker for avatar

### TicketsFragment
- My purchased tickets via `ApiManager.fetchMyPurchases()`
- RecyclerView with TicketAdapter
- Skeleton loading
- Empty state layout

---

## Data Models

### Ticket (Parcelable)

| Field | Type | Description |
|-------|------|-------------|
| firestoreId | String | Unique ID (historical name from Firebase era) |
| eventName | String | Ticket title |
| date | String | Display date |
| eventDate | String | Event date (for sorting) |
| originalPrice | double | Original face value price |
| sellingPrice | double | Resale asking price |
| sellerName | String | Seller display name |
| sellerId | String | Seller user ID |
| rating | float | Seller rating |
| status | String | `available`, `sold`, `expired` |
| imageUris | List\<String\> | Image URLs |
| ticketFileUris | List\<String\ | Ticket file URLs |

**Computed methods:**
- `getSavings()` — Returns `originalPrice - sellingPrice`
- `isDiscounted()` — Returns `sellingPrice < originalPrice`
- `getSmartImageResId()` — Keyword-based fallback: matches event name against categories (sports, concert, comedy, tech, food, etc.) and returns a local drawable resource

### WalletTransaction

| Field | Type | Description |
|-------|------|-------------|
| eventName | String | Associated ticket title |
| date | String | Transaction date |
| price | double | Amount |
| status | String | `PROCESSING` or `AVAILABLE` |
| isPurchase | boolean | True for debits, false for credits |

### ChatMessage

| Field | Type | Description |
|-------|------|-------------|
| text | String | Message content |
| isBuyer | boolean | True if sent by current user |
| time | String | Display time |
| timestamp | long | For sorting |

Custom `equals()`/`hashCode()` for deduplication during polling.

---

## Networking

### ApiClient (Singleton)

- Configures Retrofit with base URL: `https://backend-three-phi-61.vercel.app/api/`
- OkHttp client with JWT auth interceptor (adds `Authorization: Bearer <token>`)
- HTTP logging interceptor (debug builds)
- Methods: `getService()`, `isLoggedIn()`, `logout()`

### ApiService (Retrofit Interface)

Defines all REST endpoints with ~30 inner request/response model classes:

- **Auth:** `signup()`, `login()`, `refreshToken()`
- **Users:** `getProfile()`, `updateProfile()`, `getUserStats()`
- **Tickets:** `getAvailableTickets()`, `getTicketDetail()`, `createTicket()`, `getMyListings()`, `getMyPurchases()`, `buyTicket()`, `confirmEntry()`
- **Wallet:** `getBalance()`, `getTransactions()`, `getMonthlyEarnings()`
- **Chat:** `getConversations()`, `getMessages()`, `sendMessage()`
- **Upload:** `uploadImage()`

Inner models: `AuthResponse`, `TicketsResponse`, `TicketResponse`, `WalletResponse`, `TransactionsResponse`, `ConversationsResponse`, `MessagesResponse`, `StatsResponse`, `EarningsResponse`, `MessageResponse`

### ApiManager (Singleton Facade)

Wraps all `ApiService` calls with:
- Callback-based async pattern (`ApiManager.Callback<T>`)
- Error message extraction from Retrofit responses
- Null safety and default values

---

## Custom Views

### ShimmerView
- Skeleton loading animation
- Configurable rounded corners, circle mode, colors
- LinearGradient shimmer effect with ValueAnimator
- Used in HomeFragment, TicketsFragment, ProfileFragment

### SalesBarChartView
- Bar chart for monthly earnings display
- Dynamic data via `setData(List<BarData>)`
- Draws rounded bars, labels, values, baseline
- Highlights tallest bar
- "No sales data yet" fallback when empty

### LoadingDialog
- Transparent AlertDialog with spinner
- Configurable message text

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | API calls, image loading |
| `READ_EXTERNAL_STORAGE` | Pick images on API < 33 |
| `READ_MEDIA_IMAGES` | Pick images on API 33+ |
| `READ_MEDIA_VIDEO` | Pick video files on API 33+ |

---

## UI Themes

- Material Design theme (`Theme.Retix`)
- Dark mode toggle via `AppCompatDelegate.setDefaultNightMode()`
- Preference stored in `SharedPreferences`
- Custom dimensions in `res/values/dimens.xml`

---

## Known Limitations

| Area | Limitation |
|------|-----------|
| Chat | HTTP polling (3s interval), no real-time (WebSocket) |
| Wallet | HTTP polling (5s interval) for balance |
| Profile edits | Name/email/avatar changes saved to SharedPreferences only, not synced to API |
| Ratings | Star rating UI exists but `submitRating()` only shows a toast, does not persist |
| Offline | No offline support or local caching |
| Push notifications | Not implemented |
| Legacy code | `HomeActivity` and `SellTicketActivity` are superseded by fragments but still exist |
| ProGuard | Disabled in release builds |

---

## File Structure

```
app/src/main/java/com/tanish/retix/
├── ReTixApplication.java        # Application class, initializes ApiClient
├── ApiClient.java                # Singleton Retrofit client with JWT interceptor
├── ApiService.java                # Retrofit interface + ~30 inner models
├── ApiManager.java               # Singleton facade for all API calls
├── TokenManager.java             # JWT + user info storage (SharedPreferences)
├── SplashActivity.java           # Entry point, routing logic
├── OnboardingActivity.java       # 3-slide ViewPager2 intro
├── LoginActivity.java            # Email/password login
├── SignupActivity.java           # Registration form
├── MainActivity.java             # Bottom nav container for 4 fragments
├── HomeFragment.java             # Browse available tickets
├── SellFragment.java             # Create ticket listing
├── WalletFragment.java           # Wallet balance + transactions
├── ProfileFragment.java          # User profile + stats + chart
├── TicketsFragment.java          # My purchased tickets
├── TicketDetailActivity.java     # Full ticket detail + buy/chat
├── ChatActivity.java             # Buyer-seller chat (polling)
├── SellerProfileActivity.java    # Seller profile + listings
├── SettingsActivity.java         # Dark mode, edit profile, logout
├── EditProfileActivity.java      # Edit name/email/avatar (local only)
├── HomeActivity.java             # Legacy (superseded by HomeFragment)
├── SellTicketActivity.java       # Legacy (superseded by SellFragment)
├── Ticket.java                   # Parcelable ticket model
├── WalletTransaction.java        # Transaction model
├── WalletTransactionAdapter.java # RecyclerView adapter for transactions
├── TicketAdapter.java            # RecyclerView adapter for ticket cards
├── ChatMessage.java              # Chat message model
├── ChatAdapter.java              # RecyclerView adapter for chat bubbles
├── LoadingDialog.java            # Transparent loading dialog
├── ShimmerView.java              # Custom skeleton loading view
├── SalesBarChartView.java        # Custom bar chart for earnings
└── util/
    └── UiUtils.java              # Keyboard, Snackbar, animation, currency helpers
```

### Layout Resources

```
app/src/main/res/layout/
├── activity_main.xml             # Bottom nav container
├── activity_login.xml            # Login form
├── activity_signup.xml           # Signup form
├── activity_splash.xml           # Splash screen
├── activity_onboarding.xml       # ViewPager2 + dots + buttons
├── activity_ticket_detail.xml    # Ticket detail (image, prices, buttons)
├── activity_sell_ticket.xml      # Legacy sell form
├── activity_seller_profile.xml  # Seller profile + listings
├── activity_settings.xml        # Settings (dark mode, logout, about)
├── activity_edit_profile.xml    # Edit profile (avatar, name, email)
├── activity_chat.xml            # Chat (RecyclerView + input)
├── activity_home.xml            # Legacy home
├── fragment_home.xml            # Home: search + tickets + FAB
├── fragment_sell.xml             # Sell form
├── fragment_wallet.xml          # Wallet: balance + transactions
├── fragment_profile.xml         # Profile: info + stats + chart
├── fragment_tickets.xml         # My tickets list
├── item_ticket.xml              # Ticket card
├── item_ticket_shimmer.xml      # Shimmer placeholder
├── item_ticket_skeleton.xml     # Skeleton placeholder
├── item_wallet_transaction.xml  # Transaction row
├── item_chat_message.xml        # Chat bubble
├── item_onboarding_slide.xml    # Onboarding slide
├── item_profile_menu.xml        # Profile menu item
├── item_stat_card.xml           # Stat card (icon, value, label)
├── dialog_loading.xml           # Loading dialog
├── dialog_rating.xml            # Star rating dialog
├── layout_empty_state.xml       # Reusable empty state
└── profile_shimmer.xml          # Profile header shimmer
```
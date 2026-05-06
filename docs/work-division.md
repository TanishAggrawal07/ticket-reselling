# ReTix App — Work Division for 3 People

## Overview

The app is divided into three vertical slices by feature area. Each person owns their Java files, layouts, and the API endpoints they depend on. Dependencies between slices are minimal and documented below.

---

## Person 1 — Auth & Core Infrastructure

**Focus:** App foundation, networking layer, and the full authentication flow.

### Java Files (12)

| File | Purpose |
|------|---------|
| `ReTixApplication.java` | Application class, initializes ApiClient |
| `ApiClient.java` | Singleton Retrofit client, JWT interceptor |
| `ApiService.java` | Retrofit interface + all inner request/response models |
| `ApiManager.java` | Singleton facade wrapping all API calls |
| `TokenManager.java` | JWT + user info storage (SharedPreferences) |
| `SplashActivity.java` | Entry point, token check, routing |
| `OnboardingActivity.java` | 3-slide ViewPager2 intro |
| `LoginActivity.java` | Email/password login |
| `SignupActivity.java` | Registration form |
| `ShimmerView.java` | Custom skeleton loading animation view |
| `LoadingDialog.java` | Transparent loading dialog with spinner |
| `util/UiUtils.java` | Keyboard, Snackbar, animation, INR currency helpers |

### Layout Files (6)

| Layout | Used In |
|--------|---------|
| `activity_splash.xml` | SplashActivity |
| `activity_onboarding.xml` | OnboardingActivity |
| `activity_login.xml` | LoginActivity |
| `activity_signup.xml` | SignupActivity |
| `item_onboarding_slide.xml` | Onboarding ViewPager2 slides |
| `dialog_loading.xml` | LoadingDialog |

### Features Owned

- App startup and routing logic
- JWT auth flow (login, signup, token refresh, logout)
- All networking infrastructure (Retrofit, OkHttp, interceptors)
- Shared UI components used across all screens (ShimmerView, LoadingDialog, UiUtils)

### API Endpoints Owned

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/auth/signup` | POST | Register new user |
| `/api/auth/login` | POST | Authenticate user |
| `/api/auth/refresh` | POST | Refresh JWT token |

### Dependencies on Others

- **Person 2** uses `ApiManager`, `ApiClient`, `ApiService`, `TokenManager`, `ShimmerView`, `LoadingDialog`, `UiUtils` — all owned by Person 1
- **Person 3** uses the same shared components

> **Person 1 should finish first** since the networking and auth layer is a dependency for both other people.

---

## Person 2 — Ticket Marketplace

**Focus:** Browsing, searching, selling, and buying tickets — the core marketplace.

### Java Files (9)

| File | Purpose |
|------|---------|
| `MainActivity.java` | Bottom nav container, fragment switching, navigation |
| `HomeFragment.java` | Browse available tickets, search filter |
| `SellFragment.java` | Create ticket listing form |
| `TicketsFragment.java` | My purchased tickets list |
| `TicketDetailActivity.java` | Full ticket detail, buy, chat link, seller link |
| `TicketAdapter.java` | RecyclerView adapter for ticket cards |
| `Ticket.java` | Parcelable ticket model with computed fields |
| `HomeActivity.java` | Legacy standalone home (to refactor/remove) |
| `SellTicketActivity.java` | Legacy standalone sell (to refactor/remove) |

### Layout Files (12)

| Layout | Used In |
|--------|---------|
| `activity_main.xml` | MainActivity (BottomNav + FragmentContainer) |
| `activity_ticket_detail.xml` | TicketDetailActivity |
| `activity_sell_ticket.xml` | SellTicketActivity (legacy) |
| `activity_home.xml` | HomeActivity (legacy) |
| `fragment_home.xml` | HomeFragment |
| `fragment_sell.xml` | SellFragment |
| `fragment_tickets.xml` | TicketsFragment |
| `item_ticket.xml` | Ticket card in RecyclerView |
| `item_ticket_shimmer.xml` | Shimmer placeholder for ticket card |
| `item_ticket_skeleton.xml` | Skeleton placeholder for ticket card |
| `layout_empty_state.xml` | Reusable empty state (shared, but primarily used here) |
| `dialog_rating.xml` | Star rating dialog in TicketDetailActivity |

### Features Owned

- Bottom navigation and fragment management
- Ticket browsing with search/filter
- Ticket detail view with buy flow
- Create/sell ticket listing with image upload
- My purchases list
- Ticket card rendering (image loading, pricing badges, seller initial)
- Legacy screen cleanup (HomeActivity, SellTicketActivity)

### API Endpoints Owned

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/tickets` | GET | List available tickets |
| `/api/tickets/[id]` | GET | Single ticket detail |
| `/api/tickets/create` | POST | Create ticket listing |
| `/api/tickets/my-listings` | GET | Seller's own tickets |
| `/api/tickets/my-purchases` | GET | Buyer's purchased tickets |
| `/api/tickets/buy/[id]` | POST | Purchase a ticket |
| `/api/tickets/confirm-entry` | POST | Confirm ticket entry |
| `/api/upload/image` | POST | Upload event image |

### Dependencies on Others

- **Person 1:** ApiManager, ApiService, TokenManager, ShimmerView, LoadingDialog, UiUtils
- **Person 3:** Navigates to `ChatActivity` and `SellerProfileActivity` (owned by Person 3)

---

## Person 3 — Wallet, Profile & Social

**Focus:** Wallet management, user profile, chat, and settings.

### Java Files (11)

| File | Purpose |
|------|---------|
| `WalletFragment.java` | Wallet balance + transactions with polling |
| `ProfileFragment.java` | User profile, stats, sales chart |
| `ChatActivity.java` | Buyer-seller chat with polling |
| `SellerProfileActivity.java` | Seller profile + their listings |
| `SettingsActivity.java` | Dark mode, edit profile, logout, about |
| `EditProfileActivity.java` | Edit name/email/avatar |
| `WalletTransaction.java` | Transaction model |
| `WalletTransactionAdapter.java` | RecyclerView adapter for transactions |
| `ChatMessage.java` | Chat message model with deduplication |
| `ChatAdapter.java` | RecyclerView adapter for chat bubbles |
| `SalesBarChartView.java` | Custom bar chart for monthly earnings |

### Layout Files (11)

| Layout | Used In |
|--------|---------|
| `fragment_wallet.xml` | WalletFragment |
| `fragment_profile.xml` | ProfileFragment |
| `activity_chat.xml` | ChatActivity |
| `activity_seller_profile.xml` | SellerProfileActivity |
| `activity_settings.xml` | SettingsActivity |
| `activity_edit_profile.xml` | EditProfileActivity |
| `item_wallet_transaction.xml` | Transaction row |
| `item_chat_message.xml` | Chat bubble (left/right) |
| `item_profile_menu.xml` | Profile menu item |
| `item_stat_card.xml` | Stat card (icon, value, label) |
| `profile_shimmer.xml` | Shimmer placeholder for profile header |

### Features Owned

- Wallet balance display with polling refresh
- Purchase and sale transaction history
- Monthly earnings bar chart (SalesBarChartView)
- User profile viewing and editing
- Dark mode toggle with persistence
- Buyer-seller real-time chat (polling-based)
- Seller profile with their listings
- Logout flow
- Settings and about screen

### API Endpoints Owned

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/wallet/balance` | GET | Wallet balance + pending |
| `/api/wallet/transactions` | GET | Transaction history |
| `/api/wallet/monthly-earnings` | GET | Earnings by month |
| `/api/users/profile` | GET/PUT | Get/update profile |
| `/api/users/stats` | GET | Active listings, sold, earnings |
| `/api/chat/conversations` | GET | User's conversations |
| `/api/chat/messages/[id]` | GET | Messages in conversation |
| `/api/chat/send` | POST | Send a message |

### Dependencies on Others

- **Person 1:** ApiManager, ApiService, TokenManager, ShimmerView, LoadingDialog, UiUtils
- **Person 2:** Navigates from `SellerProfileActivity` to `TicketDetailActivity` (owned by Person 2)

---

## Summary

| | Person 1 | Person 2 | Person 3 |
|---|---|---|---|
| **Area** | Auth & Core | Marketplace | Wallet & Social |
| **Java files** | 12 | 9 | 11 |
| **Layout files** | 6 | 12 | 11 |
| **API endpoints** | 3 | 8 | 7 |
| **Total files** | 18 | 21 | 22 |
| **Start first** | Yes | After Person 1 | After Person 1 |

## Suggested Build Order

1. **Person 1** starts immediately — networking + auth is a hard dependency for both others
2. **Person 2 & Person 3** can start in parallel once Person 1's core is stable (ApiClient, ApiService, ApiManager, TokenManager)
3. Cross-slice navigation (Person 2 → Person 3's ChatActivity/SellerProfile, Person 3 → Person 2's TicketDetailActivity) can be wired up with intent stubs initially and connected once both slices are ready

## Shared Resources (Owned by Person 1, Used by All)

| Resource | Used By |
|----------|---------|
| `ApiManager` | Person 2, Person 3 |
| `ApiClient` + `ApiService` | Person 2, Person 3 |
| `TokenManager` | Person 2, Person 3 |
| `ShimmerView` | Person 2 (Home, Tickets), Person 3 (Profile) |
| `LoadingDialog` | Person 2 (TicketDetail, Sell), Person 3 (EditProfile) |
| `UiUtils` | Person 2, Person 3 |
| `layout_empty_state.xml` | Person 2 (Home, Tickets), Person 3 (Wallet) |
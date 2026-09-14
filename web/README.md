# Himat SMS - Admin Web Panel

Modern, responsive web interface for Himat SMS built with **React 18**, **Vite**, **Tailwind CSS**, and **Shadcn UI** (default black & white monochrome styling, cylindrical pill buttons/badges, minimalist borders), powered by **Firebase Google Authentication** and **Realtime Database** (`himatsms`).

---

## Features

1. **Shadcn UI Aesthetic**:
   - Monochrome black & white color palette (`zinc` / `slate`).
   - Cylindrical pill buttons (`rounded-full`) and pill badges.
   - Minimalist cards with subtle borders and shadows.
   - Light / Dark mode switcher.

2. **Google Authentication**:
   - One-click Google Sign-In via Firebase Auth.
   - Admin authorization check against `super_admins` in Firebase RTDB.
   - Google avatar & profile dropdown with secure sign out.

3. **Live Two-Way Data Synchronization**:
   - Connected directly to `https://himatsms-default-rtdb.firebaseio.com`.
   - Any updates made in the Android mobile app reflect immediately on the web dashboard in real time!
   - Updates made on web (payments, deliveries, new trips) sync straight to mobile devices.

4. **Complete Admin Operations**:
   - **Dashboard**: High-level financial KPIs, active trips counter, pending dues, recent activity.
   - **Market Trips**: View buyer sourcing visits, trip stop inspector, and complete trips.
   - **Orders & Purchases**: Full purchase invoice ledger with item codes, cases, loose pcs, and bill totals.
   - **Pending Operations Hub**: Real-time tracker for unpaid bills, pending consignments, active trips, and unpackaged loose pieces.
   - **Payments & Billing**: Financial ledger, outstanding balance breakdown, and inline "Record Payment" dialog.
   - **Deliveries & Dispatch**: Transport carriers, LR/Bilty assignment, and delivery status tracking.
   - **Customers Directory**: Buyer directory with phones and GSTINs.
   - **Suppliers & Mills Directory**: Textile mills and manufacturers directory.

---

## How to Run

### Development Server
```bash
cd web
npm run dev
```
Open **[http://localhost:5173](http://localhost:5173)** in your browser.

### Production Build
```bash
cd web
npm run build
```
Production assets are generated in `web/dist`.

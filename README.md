# FIXMe Android

Mobile client for the FIXMe enterprise asset & maintenance management system — PT Erlangga Edi Laboratories (Erela).

The app gives field technicians, supervisors, and managers a full-featured mobile interface for submitting and tracking maintenance cases, managing approvals, logging progress, and running AC preventive maintenance — with real-time notifications via SSE and Pusher.

---

## Screenshots

| Login | Main Menu |
|---|---|
| ![Login](docs/screenshots/login.png) | ![Main Menu](docs/screenshots/main_menu.png) |

---

## Tech Stack

| Area | Library / Tool |
|---|---|
| Language | Kotlin |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 37 |
| UI | XML Layouts, Material Design 3, ConstraintLayout |
| Architecture | MVVM (ViewModel + Repository) |
| Networking | Retrofit 3.0.0 + OkHttp 5.3.2 |
| Image Loading | Glide 5.0.7 |
| QR Scanning | ZXing Android Embedded 4.3.0 |
| Real-time | Server-Sent Events (SSE), Pusher Java Client 2.4.4 |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Maps / Location | Google Maps 20.0.0, Play Services Location 21.3.0 |
| Animations | Lottie 6.7.1 |
| In-app Updates | AppXUpdater 2.0.20 |
| Build | Gradle (AGP 9.2.1), Kotlin JVM 11 |

---

## Features

### Authentication
- Login with username and password
- Change password and email from Settings
- Session persistence via SharedPreferences

### Case / Submission Management
- Browse submission list with department and status filters
- Submit new maintenance/GA requests (category, department, location, photo)
- View full submission detail with timeline
- Edit, cancel, approve, or reject submissions
- Dual-approval chain: reporting dept manager → target dept manager
- Hold and resume issues

### Progress Tracking
- Add and update progress entries with photos
- Log material usage per progress entry
- Request material additions with approval workflow
- Mark progress as done and hand off to trial

### Trial Management
- Mark submission ready for trial
- Start and report trial results

### Technician & Supervisor Assignment
- Set and update SPV (supervisor) and field technicians per case

### AC Maintenance Module
- Scan AC unit QR code to start a maintenance session
- Check in / check out for maintenance tasks
- View task list for the current session
- Manage session participants (add/remove technicians)

### Notifications
- Inbox with all system notifications
- Real-time updates via foreground SSE service
- Firebase Cloud Messaging (configured)

### Settings
- Change password
- Change email
- App version info and in-app update check

---

## Architecture

```
app/src/main/java/com/erela/fixme/
├── activities/         # 14 screens (one Activity per screen)
├── adapters/
│   ├── recycler_view/  # RecyclerView adapters for lists
│   └── pager/          # ViewPager adapters
├── bottom_sheets/      # 15+ Material bottom sheet dialogs
├── custom_views/       # CustomToast, ActivityContainer, ImageZoomHelper
├── dialogs/            # LoadingDialog, ConfirmationDialog, PhotoPreviewDialog, etc.
├── helpers/
│   ├── api/
│   │   ├── GetEndpoint.kt      # Retrofit interface (50+ endpoints)
│   │   └── InitAPI.kt          # OkHttpClient + Retrofit setup
│   ├── UserDataHelper.kt       # SharedPreferences session manager
│   ├── PermissionHelper.kt     # Runtime permissions
│   └── NotificationsHelper.kt  # Local notification builder
├── objects/            # Response & data model classes
│   └── ac/             # AC-specific models
├── repository/
│   └── AcRepository.kt         # AC module data layer
├── services/
│   ├── SseService.kt           # Foreground SSE real-time service
│   └── FCMService.kt           # FCM message handler
└── viewmodel/
    └── AcMaintenanceViewModel.kt
```

---

## API

The app communicates with the [FIXMe Laravel backend](../FixMe-Laravel) via a REST API at:

```
{BASE_URL}apimobile/
```

| Group | Endpoints |
|---|---|
| Auth | login, changePassword, changeEmail, updateFcmToken |
| Submissions | list, detail, submit, update, cancel, approve, reject, hold, resume |
| Progress | create, list, update, delete, markDone, editProgress |
| Materials | list, requestAdd, approveRequest |
| Assignments | getSupervisors, getTechnicians, assignSPV, setupTechnicians, updateTechnicians |
| Categories / Depts | getCategoryList, getDepartmentList, getSubDepartments |
| Trial | getTrial, startTrial, reportTrial, markReadyForTrial |
| AC Maintenance | scan, checkIn, checkOut, taskList, addTechnician, removeTechnician, sessionParticipants |
| Notifications | checkInbox |

Base URLs are configured per build type in `app/build.gradle`.

---

## Requirements

- Android Studio Meerkat (2024.3) or newer
- JDK 11+
- Android device or emulator running **Android 8.0+ (API 26)**
- `local.properties` configured (see below)
- Access to the FIXMe backend server

---

## Build Setup

### 1. Clone the repository

```bash
git clone <repository-url>
cd FixMe-android
```

### 2. Configure `local.properties`

Create or edit `local.properties` in the project root:

```properties
sdk.dir=/path/to/your/Android/sdk

MAPS_API_KEY=your_google_maps_api_key

# Release signing (only needed for release builds)
KEY_STORE_PASSWORD=
KEY_ALIAS=
KEY_PASSWORD=
```

### 3. Configure `google-services.json`

Place your Firebase `google-services.json` in `app/`.

### 4. Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

Or open the project in Android Studio and run directly.

### Build Types

| Type | Base URL | Notes |
|---|---|---|
| `debug` | `http://192.168.3.245/fixme/` | Local dev server, debuggable, no minification |
| `release` | `http://182.23.21.202:8282/fixme/` | Production server, minified + ProGuard |

---

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | API and real-time communication |
| `CAMERA` | QR code scanning, photo capture |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Location tagging on submissions |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` | Photo attachment on Android 13+ |
| `POST_NOTIFICATIONS` | Push notification display |
| `FOREGROUND_SERVICE` | SSE real-time background service |
| `REQUEST_INSTALL_PACKAGES` | In-app APK update installation |

---

## Version

Current version: **1.4.0a** (build auto-incremented via `buildNumber.properties`)

---

## License

Proprietary — PT Erlangga Edi Laboratories. All rights reserved.

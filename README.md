☕ Eco Coffee Hub Manager
An Android Jetpack Compose app built for SDG 12: Responsible Consumption and Production. It helps users track coffee footprints and find sustainable beans.

🚀 Core Features (4 Pillars)
Local Persistence: Uses Room Database to save coffee bean lists permanently for offline access.

Cloud Integration: Connects to Firebase Firestore to back up eco-data and share community stats.

Internet Data (API): Uses Retrofit to fetch live, dynamic product details from the OpenFoodFacts REST API.

Sensor Integration: Uses the phone's Hardware Accelerometer to detect shaking gestures and recommend sustainable beans.

📱 7-Screen Navigation
All screens consistently use a bottom navigation bar and can be reached in 2 clicks or fewer:

Home (🌍): SDG 12 vision introduction.

Brew (☕): Prep dashboard.

Add (➕): Manual input & Shake-to-recommend feature.

List (📋): Offline items from local Room DB.

API (🔍): Barcode search with live REST API loading.

Impact (🌱): Eco score tracking and cloud sync gate.

Cloud (☁️): Global shared community hub from Firebase.

🛠️ Setup Instructions
Clone project: git clone <your-repo-url>

Open in Android Studio (Ladybug/Jellyfish or newer).

Add Firebase: Download google-services.json and put it inside the /app folder.

Sync & Run: Wait for Gradle sync, select an emulator (API 30+), and click Run (▶).

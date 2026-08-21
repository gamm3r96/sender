# 📡 SENDER — Air-Gapped End-to-End Encrypted File & Secret Transfer

[![Platform](https://img.shields.io/badge/Platform-Android%2014+-3DDC84?style=flat&logo=android)](https://www.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=flat&logo=kotlin)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Encryption](https://img.shields.io/badge/Encryption-AES--256--GCM-00E5FF?style=flat)](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
[![Author](https://img.shields.io/badge/Developer-Elvis%20Gatwara%20(%40gamm3r96)-10B981?style=flat&logo=github)](https://github.com/gamm3r96)
[![Portfolio](https://img.shields.io/badge/Portfolio-elvis--gatwara.vercel.app-blue?style=flat)](https://elvis-gatwara.vercel.app)
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/R6R71ERSUM)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

<p align="center">
  <img src="docs/screenshots/hero_banner.jpg" alt="Sender Air-Gapped Encrypted Transfer Suite" width="100%" style="border-radius: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.5);" />
</p>

> **Sender** is an ultra-secure, zero-knowledge, offline-first Android application designed for transmitting files, cryptographic keys, secrets, and raw binaries across physical boundaries using **high-frequency animated QR code streams** and **zero-cloud encrypted local P2P**.

---

## 📑 Table of Contents

- [🌟 Key Capabilities](#-key-capabilities)
- [📸 Screenshots & Interface Showcase](#-screenshots--interface-showcase)
- [📖 Comprehensive Step-by-Step Usage Guide](#-comprehensive-step-by-step-usage-guide)
  - [1. Optical Air-Gapped File & Secret Transmission](#1-optical-air-gapped-file--secret-transmission)
  - [2. Receiving & Reassembling the Animated QR Stream](#2-receiving--reassembling-the-animated-qr-stream)
  - [3. Wi-Fi / Personal Hotspot Encrypted Transfer & LAN Discovery](#3-wi-fi--personal-hotspot-encrypted-transfer--lan-discovery)
  - [4. Cross-Platform Web Browser Drop Portal (Zero Install)](#4-cross-platform-web-browser-drop-portal-zero-install)
  - [5. Team Keyrings & Biometric Cryptographic Security](#5-team-keyrings--biometric-cryptographic-security)
  - [6. Optical Matrix Customization & Glare Compensation](#6-optical-matrix-customization--glare-compensation)
  - [7. Offline Peer-to-Peer APK Provisioning](#7-offline-peer-to-peer-apk-provisioning)
  - [8. Forensic Verification & Emergency Data Purge](#8-forensic-verification--emergency-data-purge)
- [📐 System Architecture & Cryptographic Pipeline](#-system-architecture--cryptographic-pipeline)
- [🛠️ Tech Stack](#️-tech-stack)
- [🚀 Building & Deployment](#-building--deployment)
- [💻 Push to Your GitHub Account](#-push-to-your-github-account)
- [👨‍💻 Author & Contact](#-author--contact)
- [📄 License](#-license)

---

## 🌟 Key Capabilities

### 📴 True Air-Gapped Optical Streaming
- **Animated QR Code Video Streams**: Transmit files, cryptographic keys, credentials, and encrypted notes without Wi-Fi, Bluetooth, cellular data, or physical cables.
- **Fountain Chunker & Resilient Sequencing**: Files are broken into sequential binary envelopes and continuously cycled at up to 24 frames per second (FPS).
- **CameraX Scanning Engine**: Continuous barcode capture with missing-packet tracking and instant atomic reassembly.

### 🎨 Optical Matrix Tuning & Glare Assist
- **6 Color Palettes**: *Mono High-Contrast*, *OLED Dark*, *Cyber Emerald*, *Electric Cyan*, *Night Vision Amber*, and *Cyber Violet*.
- **Selectable Error Correction Levels (ECC)**:
  - `Level L`: 7% recovery, maximum payload density
  - `Level M`: 15% recovery, balanced standard
  - `Level Q`: 25% recovery, glare & vibration tolerant
  - `Level H`: 30% recovery, maximum recovery for scratched or sunlit screens
- **Custom Module Geometries**: Sharp Squares, Rounded Matrix Blocks, and Smooth Circular Dots.
- **Scanner Optical Enhancements**: Real-time viewfinder contrast filters (Normal, High Contrast, Solarized, Inverted) for scanning under difficult lighting.

### 🔒 Military-Grade Cryptography
- **AES-256-GCM Authenticated Encryption**: 128-bit authentication tags and 12-byte initialization vectors (IV) guarantee zero tampering.
- **PBKDF2 Key Derivation**: SHA-256 with 100,000 iterations and unique 16-byte cryptographic salts per envelope.
- **Zero-Cloud Guarantee**: All encryption, decryption, and fragmentation occur strictly in volatile device RAM.

---

## 📸 Screenshots & Interface Showcase

<p align="center">
  <img src="docs/screenshots/features_showcase.jpg" alt="Features Showcase - Optical Stream & CameraX Scanner" width="49%" style="border-radius: 8px;" />
  <img src="docs/screenshots/security_vault.jpg" alt="Security Vault - Team Keyring & Biometrics" width="49%" style="border-radius: 8px;" />
</p>

---

## 📖 Comprehensive Step-by-Step Usage Guide

### 1. Optical Air-Gapped File & Secret Transmission

Use this mode to transmit sensitive files or encrypted notes across physical space without leaving any wireless trace.

```
[ Choose Mode ] ──> [ Select File / Type Note ] ──> [ Enter Passphrase / Select Key ] ──> [ Stream QR Frames ]
```

1. **Open the Send Tab**: Navigate to the `Transmit` screen.
2. **Choose Payload Type**:
   - **File Mode**: Tap **Choose File** to pick any document, photo, archive, or binary.
   - **Secret Note Mode**: Switch to the **Secret Note** tab and enter confidential text or credentials.
3. **Set Cryptographic Password**:
   - Enter a custom one-time passphrase, OR
   - Tap **Select Team Key** to pick a pre-shared cryptographic key from your encrypted keyring.
4. **Initiate Transmission**:
   - Tap **Generate Encrypted Stream**.
   - The app derives a 256-bit key via PBKDF2 (100,000 rounds), encrypts the payload via AES-256-GCM, fragments it into structured binary chunks, and initiates the animated high-frequency QR loop.
5. **Adjust Stream Controls**:
   - Use the **FPS Slider** (1 to 24 FPS) to match the receiver camera's shutter speed.
   - Tap **Pause / Play** to halt on a specific chunk if needed.
   - Use **Prev / Next** or **Jump to Last** for manual step-by-step frame inspection.
   - Tap **Fullscreen** for unobstructed optical transmission.

---

### 2. Receiving & Reassembling the Animated QR Stream

```
[ Point Camera ] ──> [ Auto-Detect Frames ] ──> [ Fill Progress Grid ] ──> [ Enter Decryption Passphrase ] ──> [ Export File ]
```

1. **Open the Receive Tab**: Navigate to the `Scanner` screen and grant Camera permission.
2. **Aim Camera at the Sending Device**:
   - Position the viewfinder over the animated QR code on the transmitting screen.
   - The scanner immediately detects incoming chunk headers and locks onto the transmission ID.
3. **Monitor Real-Time Reassembly**:
   - The interactive **Chunk Matrix Grid** visually illuminates each received block in emerald green.
   - The progress percentage, received bytes, and missing packet count update in real time.
4. **Complete Capture & Decrypt**:
   - As soon as all unique packets are collected (100%), reassembly completes automatically with haptic feedback.
   - Enter the transmission passphrase or select the corresponding Team Key to decrypt.
5. **Save or Inspect**:
   - For files: Tap **Save File** to export directly to Android storage or tap **Share** to forward to other apps.
   - For secret notes: Tap **Copy to Clipboard** or review the raw payload safely.

---

### 3. Wi-Fi / Personal Hotspot Encrypted Transfer & LAN Discovery

When files are large (e.g. 50MB–2GB+ videos, disk images, APKs, or full databases), switch seamlessly to high-speed **Air-Gapped Wi-Fi / Hotspot Mode**:

```
[ Sender: Start Wi-Fi Server ] ──> [ LAN Auto-Discovery / Optical Ticket ] ──> [ AES-256-GCM Direct Stream ]
```

1. **Establish Offline Network**:
   - Both devices connect to the same local Wi-Fi router, OR
   - Device A turns on **Android Portable Hotspot** (via the quick in-app button) and Device B joins it. No internet connection is needed!
2. **Sending Over Wi-Fi / Hotspot**:
   - Select your file in the **Transmit** tab.
   - Switch transfer mode to **Wi-Fi / Hotspot Direct**.
   - Enter your encryption passphrase or select a Team Key.
   - Tap **Start Encrypted Wi-Fi / Hotspot Server**.
   - An ephemeral, ultra-fast server starts on port `8989`, broadcasting an optical connection ticket and LAN beacon.
3. **Receiving Over Wi-Fi / Hotspot**:
   - **Method A (Subnet Radar Scan)**: Open **Scanner** -> switch to the **Wi-Fi / Hotspot** tab -> tap **Scan Subnet**. Sender hosts appear automatically. Tap **Download & Decrypt**.
   - **Method B (Optical Ticket)**: Aim the camera at the sender's screen to scan the connection ticket QR code for instantaneous zero-input pairing.
   - **Method C (Manual IP / Port)**: Enter the sender's IP address (e.g. `192.168.43.1:8989`) and decrypt directly.

---

### 4. Cross-Platform Web Browser Drop Portal (Zero Install)

Transfer files to or from laptops, iPhones, Linux workstations, or Macs without installing any software on the target device:

1. **Sender-to-Browser**:
   - Start the Wi-Fi / Hotspot Server on Android.
   - The app displays a direct web link (e.g. `http://192.168.43.1:8989/`).
   - Open that URL in Chrome, Safari, or Firefox on any computer or phone connected to the same Wi-Fi/Hotspot to download the payload via a sleek cyber-styled web portal.
2. **Browser-to-Android (Receiver Web Drop)**:
   - On your Android phone, go to **Scanner** -> **Wi-Fi / Hotspot** -> tap **Start Receiver Web Drop Server**.
   - The phone displays its receiver URL (e.g. `http://192.168.43.1:8990/`).
   - Anyone on the LAN can open the browser page, drag & drop files into the phone, and they are saved directly into your phone's encrypted vault.

---

### 5. Team Keyrings & Biometric Cryptographic Security

Eliminate the need to re-type long passwords during high-tempo field operations by configuring pre-shared cryptographic team keys.

1. **Manage Team Keys**:
   - Navigate to `Settings` -> `Team Keyring Management`.
   - Tap **Add Key** to store a named cryptographic key (e.g. `Alpha-Team-Operations`, `Field-Agent-07`).
2. **Biometric Vault Protection**:
   - Toggle **Biometric Vault Protection** in Settings.
   - Accessing sensitive keys, changing security policies, or launching the app will require fingerprint or face authentication.
3. **One-Tap Key Selection**:
   - When sending or receiving, simply select the team key from the dropdown to automatically derive matching AES-256-GCM encryption parameters.

---

### 6. Optical Matrix Customization & Glare Compensation

Optimize optical reading performance based on environmental lighting conditions.

| Setting | Options | Best Used For |
| :--- | :--- | :--- |
| **Color Scheme** | `Mono High-Contrast`, `OLED Dark`, `Cyber Emerald`, `Electric Cyan`, `Night Vision Amber`, `Cyber Violet` | Low-light tactical environments, battery saving, LCD/OLED anti-glare |
| **Error Correction (ECC)** | `Level L (7%)`, `Level M (15%)`, `Level Q (25%)`, `Level H (30%)` | Level H for cracked/scratched screens or direct outdoor sunlight; Level L for maximum transmission speed |
| **Module Shape** | `Square`, `Rounded`, `Circle` | Visual aesthetics and scan contrast tuning |
| **Scanner Contrast Assist** | `Normal`, `High Contrast`, `Solarized`, `Inverted` | Overcoming aggressive screen reflections or dark ambient lighting |
| **Brightness Boost** | `Enabled / Disabled` | Automatically ramps screen brightness to 100% during active transmission |

---

### 7. Offline Peer-to-Peer APK Provisioning

Deploy Sender to new field devices without Google Play, app stores, or internet access:

1. Navigate to `Settings` -> `Offline App Distribution`.
2. Tap **Export Sender APK**.
3. Choose **Share APK directly via Bluetooth / Nearby / File Share** to provision unequipped companion devices instantly.

---

### 8. Forensic Verification & Emergency Data Purge

1. **Audit Logs**:
   - Visit the `History / Audit` screen to review all past transmissions and receptions.
   - Inspect exact cryptographic checksums (SHA-256), payload sizes, encryption tags, and timestamps.
2. **Emergency Purge**:
   - Tap the red **Emergency Purge** button in History or Settings.
   - Instantly wipes all cryptographic audit records, saved team keys, cached payloads, and temporary files from device storage.

---

## 📐 System Architecture & Cryptographic Pipeline

```
[ Sender Device ]                                      [ Receiver Device ]
       │                                                       │
  Plaintext File / Secret                                      │
       │                                                       │
  CryptoManager (PBKDF2 + AES-256-GCM)                         │
       │                                                       │
  Binary Envelope Serialization (Header + Salt + IV + Data)    │
       │                                                       │
  Fountain Chunker (Density Preset / ECC Configuration)        │
       │                                                       │
  Animated QR Stream (1–24 FPS, Custom Geometry)               │
       │ ─── [ Optical Air-Gap / Camera Viewfinder ] ────────> │
       │                                                 CameraX Analyzer
       │                                                       │
       │                                                 Missing-Chunk Tracker
       │                                                       │
       │                                                 Reassembly Buffer
       │                                                       │
       │                                                 Envelope Authenticator
       │                                                       │
       │                                                 AES-GCM Decryption
       │                                                       │
                                                         Verified File / Note
```

---

## 🛠️ Tech Stack

- **Language**: [Kotlin 2.0](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Camera & Scanning**: [AndroidX CameraX](https://developer.android.com/training/camerax) + [ZXing](https://github.com/zxing/zxing)
- **Local Persistence**: [Android Room Database](https://developer.android.com/training/data-storage/room) with KSP
- **Security & Biometrics**: [AndroidX Biometric](https://developer.android.com/jetpack/androidx/releases/biometric) + `javax.crypto.Cipher` (AES-256-GCM)
- **Asynchronous Pipeline**: Kotlin Coroutines, `StateFlow`, `SharedFlow`

---

## 🚀 Building & Deployment

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- Android SDK 34+
- Java JDK 17+

### Build via Command Line

```bash
# Clone the repository
git clone https://github.com/gamm3r96/sender.git
cd sender

# Assemble debug APK
./gradlew assembleDebug

# Install directly to USB / Wi-Fi connected device
./gradlew installDebug
```

---

## 💻 Push to Your GitHub Account (@gamm3r96)

To push this repository to your personal GitHub account (`@gamm3r96`):

```bash
# 1. Initialize git (if not already initialized)
git init

# 2. Stage all files
git add .

# 3. Commit changes
git commit -m "feat: complete Sender air-gapped encrypted transfer suite with documentation"

# 4. Set main branch
git branch -M main

# 5. Add your remote repository URL
git remote add origin https://github.com/gamm3r96/sender.git

# 6. Push to GitHub
git push -u origin main
```

---

## 👨‍💻 Author & Contact

**Elvis Gatwara** (`@gamm3r96`)
- 🐙 **GitHub**: [@gamm3r96](https://github.com/gamm3r96)
- 🌐 **Portfolio & Web**: [elvis-gatwara.vercel.app](https://elvis-gatwara.vercel.app)
- ✉️ **Email**: [elvisgatwara@gmail.com](mailto:elvisgatwara@gmail.com)

### ☕ Support & Sponsorship

If you find this project useful for air-gapped security, zero-trust protocols, or offline data distribution, consider buying me a coffee:

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/R6R71ERSUM)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


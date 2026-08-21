# 📡 SENDER — Air-Gapped End-to-End Encrypted File & Secret Transfer

[![Platform](https://img.shields.io/badge/Platform-Android%2014+-3DDC84?style=flat&logo=android)](https://www.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=flat&logo=kotlin)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Encryption](https://img.shields.io/badge/Encryption-AES--256--GCM-00E5FF?style=flat)](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
[![Author](https://img.shields.io/badge/Developer-Elvis%20Gatwara-10B981?style=flat)](https://elvis-gatwara.vercel.app)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Sender** is an ultra-secure, zero-knowledge, offline-first Android application designed for transmitting files, secrets, and raw binaries across physical boundaries using **high-frequency animated QR code streams** and **zero-cloud encrypted local P2P**.

---

## 🌟 Key Features

### 1. 📴 True Air-Gapped Optical Transfer
- **Animated QR Code Video Streams**: Transmit files, photos, cryptographic keys, and encrypted text across devices without Wi-Fi, Bluetooth, cellular data, or physical cables.
- **Fountain Chunker & Resilient Sequencing**: Files are broken down into sequential binary envelopes and continuously cycled at up to 24 frames per second (FPS).
- **CameraX Scanning Pipeline**: High-speed real-time continuous barcode capture with intelligent missing-packet recovery and instant reassembly.

### 2. 🎨 Advanced Optical Matrix Tuning & Error Correction
- **Dynamic Color Schemes**:
  - `Mono High-Contrast` (Universal Black & White)
  - `OLED Dark` (Inverted battery-efficient matrix)
  - `Cyber Emerald` (High-luminescence signature matrix)
  - `Electric Cyan` (Sharp anti-glare for LCD screens)
  - `Night Vision Amber` (Low-glare low-light transfer)
  - `Cyber Violet` (High-saturation ultraviolet matrix)
- **Selectable Error Correction Levels (ECC)**:
  - `Level L`: 7% recovery, ultra-dense payload capacity
  - `Level M`: 15% recovery, balanced standard
  - `Level Q`: 25% recovery, glare & vibration tolerant
  - `Level H`: 30% recovery, maximum recovery for scratched or sunlit screens
- **Custom Module Geometry**: Sharp Squares, Rounded Matrix Blocks, and Smooth Circular Dots.
- **Camera Scanner Contrast Assist**: Viewfinder optical enhancements (Normal, High Contrast, Solarized, Inverted) for scanning under difficult lighting.

### 3. 🔒 Military-Grade Cryptography (AES-256-GCM)
- **Zero-Knowledge Encryption**: All payloads are encrypted locally on the sender's device before leaving RAM.
- **PBKDF2 Key Derivation**: Uses SHA-256 with 100,000 iterations and a unique 16-byte cryptographic salt per envelope.
- **Authenticated Encryption**: 128-bit authentication tag with 12-byte initialization vectors (IV) guarantees immunity against tampering or replay attacks.
- **Cryptographic Team Keyring**: Store named pre-shared keys (PSK) with biometric gating for rapid recurring team workflows.

### 4. ⚡ High-Speed Air-Gapped Local P2P Fallback
- For larger binary files (videos, APKs, databases), effortlessly spin up an embedded, encrypted HTTP transfer daemon.
- Generates an encrypted one-time optical connection ticket for instant receiver discovery and verification without internet access.

### 5. 🛡️ Biometric Vault & Local Auditing
- Lock the app and cryptographic keys behind Android Biometric Prompt (Fingerprint / Face ID / Custom PIN fallback).
- Full cryptographic audit history with file inspection, hash verification (SHA-256 checksums), and one-tap emergency purge.
- **Offline APK Self-Distribution**: Export and share the Sender APK peer-to-peer to provision new devices in the field.

---

## 📐 System Architecture

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

## 🛠️ Tech Stack & Dependencies

- **Language**: [Kotlin 2.0](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Camera & Scanning**: [AndroidX CameraX](https://developer.android.com/training/camerax) + [ZXing (Zebra Crossing)](https://github.com/zxing/zxing)
- **Local Persistence**: [Android Room Database](https://developer.android.com/training/data-storage/room) with Kotlin Symbol Processing (KSP)
- **Security & Biometrics**: [AndroidX Biometric](https://developer.android.com/jetpack/androidx/releases/biometric) + Java Cryptography Extension (`AES/GCM/NoPadding`)
- **Coroutines & Asynchronous Streams**: Kotlin Coroutines & `StateFlow` / `SharedFlow`

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- Android SDK 34+
- Java JDK 17+

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/sender.git
   cd sender
   ```

2. **Open in Android Studio** or build via Gradle command-line:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on connected device**:
   ```bash
   ./gradlew installDebug
   ```

---

## 💻 Push to Your GitHub Account

To push this repository to your personal GitHub account:

```bash
# 1. Initialize git (if not already done)
git init

# 2. Stage all files
git add .

# 3. Commit changes
git commit -m "feat: complete Sender air-gapped encrypted transfer suite with QR customization"

# 4. Set main branch
git branch -M main

# 5. Add your remote repository URL
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/sender.git

# 6. Push to GitHub
git push -u origin main
```

---

## 👨‍💻 Author & Contact

**Elvis Gatwara**
- 🌐 **Portfolio & Web**: [elvis-gatwara.vercel.app](https://elvis-gatwara.vercel.app)
- ✉️ **Email**: [elvisgatwara@gmail.com](mailto:elvisgatwara@gmail.com)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

# 📱 Smart Notes: Decentralized AI & Offline Voice Vault

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![F-Droid](https://img.shields.io/badge/F--Droid-Submission_Pending-green.svg)](https://gitlab.com/fdroid/rfp)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-green.svg)]()

Break free from corporate data harvesting and cloud subscriptions. Smart Notes is a 100% open-source, offline-capable productivity vault designed for power users, developers, and privacy advocates. 

Built entirely from a mobile terminal environment, Smart Notes combines a raw hardware audio engine with a completely decentralized AI architecture. **Your data, your keys, your rules.**

---

## 🔥 Official v2.0 Features

* **The Local Vault Export (NEW):** Instantly export your entire secure database as a clean Markdown (`.md`) file directly to your device's local storage. Zero lock-in.
* **Native Offline Voice AI:** Flip the "Local Whisper" toggle to sever all network connections. Smart Notes uses a natively compiled C++ NDK engine to rip raw 16kHz audio straight from your hardware microphone and translate it locally. Zero cloud, zero tracking.
* **Bring Your Own Key (BYOK) Router:** Stop paying expensive monthly AI subscriptions. Plug in your own API keys for Groq (FOSS models), Google AI Studio, or OpenAI. You only pay fractions of a cent for exactly what you use.
* **Local Encrypted Vault:** All transcriptions, notes, and AI responses are secured inside a local SQLite Room Database. Your data never leaves your device unless you choose to export it.
* **Strictly Open Source:** No proprietary trackers, no forced telemetry, and no ad SDKs. 

---

## 🛠️ Technical Stack & Build Info
This application was developed dynamically using mobile-first workflows (Termux on Android). 

* **UI:** Kotlin & Jetpack Compose
* **Database:** Room (KAPT configured)
* **Voice Engine:** Whisper.cpp (ggml-tiny.en)
* **Target:** Java 17 / Android SDK 34
* **Metadata:** F-Droid Fastlane compliant

### Building from Source
To compile this application locally using Gradle:
```bash
git clone [https://github.com/BigWizard94/smart-notes-app.git](https://github.com/BigWizard94/smart-notes-app.git)
cd smart-notes-app

Note: Due to KAPT compiler restrictions with Room, this project must be built using JDK 17.

Developed by Bigwizard Media. Empowering users with transparent, decentralized, and locally hosted toolkits.

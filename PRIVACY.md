# 🔒 Privacy & Security

Smart Notes is built with a "Privacy-First" philosophy. As a FOSS (Free and Open Source Software) application, transparency is our priority.

## 🛡️ Data Handling
* **Zero Telemetry:** This app does not contain trackers, analytics, or crash-reporting SDKs. We do not collect, store, or transmit any data about your usage.
* **Offline Only:** All notes and recordings are stored locally on your device. No data is ever uploaded to a cloud server unless you manually share a file through an external app.
* **No Proprietary Locks:** Your data is yours. We use standard Android file formats to ensure no vendor lock-in.

## 🎙️ Permissions Transparency
Smart Notes requests the following permissions only when necessary:

| Permission | Purpose |
| :--- | :--- |
| `RECORD_AUDIO` | Required to create voice-to-text or audio notes. Audio is processed locally and is never sent to any external servers. |
| `INTERNET` | Used strictly for optional network-based features (like syncing via user-defined paths or GitHub integration). The app remains fully functional offline. |

## 💾 Backup Security
The app utilizes the standard `android:allowBackup` feature to help you preserve your data during device migrations. Please be aware that if you have Google Cloud Backup enabled on your device, your local database may be included in your system backups.

## 📜 License
This project is licensed under the **GNU General Public License v3.0 (GPLv3)**, ensuring that the software remains free and open for everyone to inspect, modify, and share.

---

## 📋 Audit & Compliance

Smart Notes has undergone a comprehensive FOSS compliance audit. You can review the full audit report here:
* **[Full FOSS Audit Report](FOSS_AUDIT_REPORT.md)**

### Compliance Status: ✅ **FULLY COMPLIANT**
- ✅ No telemetry or tracking
- ✅ No proprietary dependencies
- ✅ Minimal, justified permissions
- ✅ Full source code transparency
- ✅ GPLv3 licensed

---

**Questions about privacy?** Open an issue on our [GitHub repository](https://github.com/BigWizard94/smart-notes-app) and we'll be happy to answer.

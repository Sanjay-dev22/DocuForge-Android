# 📄 DocuForge — Android Document Engine

## 🚀 Overview

DocuForge is a modern Android application that supports viewing multiple document formats using a hybrid **local + cloud-powered rendering pipeline**.

It handles complex formats like PPT by converting them into viewable PDFs using a cloud API, then caching results locally for performance.

---

## ✨ Features

### 📄 Multi-Format Support

* PDF Viewer (native rendering)
* TXT Viewer (infinite scroll + search)
* Excel Viewer (Apache POI)
* PPT/PPTX Viewer (cloud-based conversion)

---

### ⚙️ Smart Rendering Pipeline

For unsupported formats (like PPT):

```bash
PPT → CloudConvert API → PDF → Local Cache → Viewer
```

* Converts documents to a renderable format
* Stores converted files locally
* Avoids repeated API calls

---

### ⚡ Performance Optimizations

* Local caching of converted files
* Reduced API calls
* Smooth UI rendering
* Progress tracking during conversion

---

## 🧠 Architecture

* UI Layer → Jetpack Compose
* Processing Layer → File handling + conversion logic
* Network Layer → CloudConvert API
* Storage Layer → Local cache for converted files

---

## 🛠️ Tech Stack

* Kotlin
* Jetpack Compose
* OkHttp (networking)
* Apache POI (Excel parsing)
* Android PDF Viewer
* CloudConvert API

---

## 🔐 Setup

Create a `local.properties` file:

```bash
CLOUD_CONVERT_API_KEY=your_api_key_here
```
---

## ⚠️ Notes

* PPT conversion requires internet
* Large files may take time due to cloud processing
* Cached files improve performance on repeat access

---

## 🚀 Future Improvements

* Offline PPT rendering
* Slide preview thumbnails
* Multi-tab document support
* File annotation (highlight, notes)
* Cloud storage integration (Drive, Dropbox)

---

## 🎯 Key Highlights

* Hybrid cloud-local processing architecture
* Handles complex document formats efficiently
* Designed for performance and scalability
* Demonstrates real-world system design thinking

---

## 📄 License

MIT License

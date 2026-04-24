# 📄 DocuForge-Android

A modern Android document viewer supporting multiple file formats with cloud-powered conversion.

## 🚀 Features

* 📄 PDF Viewer (native rendering)
* 📝 TXT Viewer (infinite scroll + search)
* 📊 Excel Viewer
* 📽️ PPT/PPTX Viewer (CloudConvert → PDF pipeline)

## ⚡ Tech Stack

* Kotlin + Jetpack Compose
* Android PDF Viewer
* Apache POI (Excel)
* CloudConvert API (PPT support)
* OkHttp

## 🧠 Architecture

PPT → CloudConvert → PDF → Local Cache → Viewer

## 🔐 Setup

Create a `local.properties` file:

```
CLOUD_CONVERT_API_KEY=your_api_key_here
```

## 📌 Notes

* PPT conversion requires internet
* Large files may take time due to cloud processing

## 🔥 Future Improvements

* PDF caching (avoid re-conversion)
* Progress tracking UI
* Offline PPT rendering
* Slide thumbnail preview

---

Built as part of a high-performance document rendering system.

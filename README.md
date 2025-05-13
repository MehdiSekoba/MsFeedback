# MsFeedback

MsFeedback is an easy-to-integrate Android library that provides a modern, Material 3-based feedback form. It allows users to submit suggestions, bug reports, and general feedback — including text, images, and optional system info — directly via email.

<p align="center">
  <img src="https://github.com/MehdiSekoba/MsFeedback/blob/main/art/art.jpg?raw=true" alt="MsFeedback Screenshot" width="400"/>
</p>

## ✨ Features

* 🔧 Fully customizable feedback form
* 📎 Image attachment support (up to 3 images)
* 🧠 Optionally include system and device information
* 📧 Send feedback via email with attachments
* 🎨 Material Design 3 UI
* 🌗 Day/Night theme support
* 🌍 Multilingual with RTL layout support (English, Persian, Arabic, Korean, Turkish)

---

## 📦 Installation

### Step 1: Add JitPack Repository

In your project-level settings.gradle:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
      maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add the Library Dependency

In your module-level build.gradle:

```kotlin
dependencies {
    implementation ("com.github.MehdiSekoba:MsFeedback:1.0.0")
}
```

## 🚀 Usage

### Basic Setup

```kotlin
import ir.mehdisekoba.feedback.MsFeedback

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MsFeedback.Builder(this)
            .withEmail("webmahdi72@gmail.com") // Required
            .withSystemInfo() // Optional
            .build()
            .start()
    }
}
```

### Builder Options

* withEmail(email: String) → Required: sets the feedback recipient email
* withSystemInfo() → Optional: includes device/system info in the feedback
* build() → Builds the feedback object
* start() → Launches the feedback form

---

## 🛡 Permissions

If you use withSystemInfo(), declare the following in your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
```

The library requests permission automatically at runtime.

---

## 📁 FileProvider Configuration

To allow file (image) attachments:

### AndroidManifest.xml

```xml
<application>
    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="ir.mehdisekoba.feedback.provider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths" />
    </provider>
</application>
```

### res/xml/file\_paths.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-cache-path name="external_cache" path="." />
</paths>
```

## 📋 Requirements

* Min SDK: 24 (Android 7.0)
* Compile SDK: 35

---

## 🤝 Contributing

Pull requests are welcome!

1. Fork the repo
2. Create a branch: git checkout -b feature/your-feature
3. Commit your changes: git commit -m "Add feature"
4. Push to the branch: git push origin feature/your-feature
5. Open a Pull Request

---

## 📧 Contact

For issues, please open a GitHub issue.
For questions or feature requests, contact: [webmahdi72@gmail.com](mailto:webmahdi72@gmail.com)

---
### **License**  
```
   Copyright (C) 2024 Mehdi Sekoba

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```


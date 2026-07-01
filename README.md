# 📖 Dynamic Book Reader

একটি সম্পূর্ণ ডেটা-চালিত Android বুক রিডার অ্যাপ — Kotlin, Jetpack Compose এবং MVVM আর্কিটেকচার দিয়ে তৈরি।

---

## ✨ মূল বৈশিষ্ট্যসমূহ

| বৈশিষ্ট্য | বিবরণ |
|---|---|
| **১০০% ডেটা-চালিত** | `book_data.json` ও `author.json` পরিবর্তন করলে পুরো অ্যাপ আপডেট হয় |
| **MVVM আর্কিটেকচার** | Clean, modular, testable কোড |
| **Material 3 UI** | Modern, polished Bengali-friendly UI |
| **ইন-মেমরি ক্যাশ** | JSON একবার parse হয়ে সেশনজুড়ে cached থাকে — instant navigation |
| **Splash Screen** | JSON থেকে বইয়ের নাম dynamically দেখায় |
| **Hero + লেখক পরিচিতি** | বই ও লেখকের সংক্ষিপ্ত তথ্য, "আরো পড়ুন" থেকে সম্পূর্ণ জীবনী |
| **৩-ট্যাব বটম নেভিগেশন** | হোম / সার্চ / মেনু |
| **স্থানীয় সার্চ** | অধ্যায়ের শিরোনাম ও বিষয়বস্তুতে অফলাইন সার্চ |
| **Lazy Chapter Rendering** | প্যারাগ্রাফ-লেভেল `LazyColumn` — বড় অধ্যায়েও স্মুথ স্ক্রল |
| **Reading Progress** | স্বয়ংক্রিয় প্রোগ্রেস ট্র্যাকিং + "শেষ পঠিত অংশে যান" শর্টকাট |
| **Loading/Error states** | প্রতিটি ডেটা লোডে আলাদা loading spinner ও error+retry UI |
| **DataStore** | পাঠের পছন্দ ও প্রোগ্রেস সেশন পেরিয়ে সংরক্ষিত থাকে |
| **CI/CD** | GitHub Actions দিয়ে স্বয়ংক্রিয় APK বিল্ড |

---

## 📁 প্রজেক্ট স্ট্রাকচার

```
DynamicBookReader/
├── .github/workflows/android.yml   ← GitHub Actions CI/CD
├── app/src/main/
│   ├── assets/
│   │   ├── book_data.json          ← ✅ বইয়ের কন্টেন্ট
│   │   └── author.json             ← ✅ লেখকের তথ্য
│   └── java/com/dynamicbookreader/
│       ├── data/
│       │   ├── model/BookData.kt          ← Chapter, Author models
│       │   └── repository/
│       │       ├── BookRepository.kt        (in-memory cached)
│       │       ├── AuthorRepository.kt       (in-memory cached)
│       │       ├── ReadingPreferencesRepository.kt
│       │       └── ReadingProgressRepository.kt
│       ├── utils/JsonParser.kt
│       ├── viewmodel/BookViewModel.kt      ← সব UI state এখানে
│       └── ui/
│           ├── theme/
│           ├── components/BottomNavBar.kt
│           ├── screens/
│           │   ├── SplashScreen.kt
│           │   ├── HomeScreen.kt            (hero + continue-reading + list)
│           │   ├── SearchScreen.kt
│           │   ├── MenuScreen.kt
│           │   ├── ReadingScreen.kt         (lazy paragraphs + progress)
│           │   ├── AuthorDetailScreen.kt
│           │   ├── SettingsScreen.kt
│           │   ├── ContactScreen.kt
│           │   ├── PrivacyPolicyScreen.kt
│           │   └── AboutScreen.kt
│           ├── AppNavigation.kt
│           └── Screen.kt
├── gradle/libs.versions.toml
├── .gitignore
└── README.md
```

---

## 📝 JSON ফাইল পরিবর্তনের নিয়ম

`app/src/main/assets/book_data.json` ফাইলটি সম্পাদনা করুন:

```json
{
  "book_title": "আপনার বইয়ের নাম",
  "data": [
    {
      "chapter_no": 1,
      "title": "অধ্যায়ের শিরোনাম",
      "subtitle": "ঐচ্ছিক সংক্ষিপ্ত উপশিরোনাম",
      "page_range": "৩৫–৪২",
      "content": "সম্পূর্ণ অধ্যায়ের বিষয়বস্তু এখানে লিখুন।\n\nনতুন অনুচ্ছেদের জন্য \\n ব্যবহার করুন।\n\nটীকা যোগ করতে {{note:1}} লিখুন।",
      "footnotes": [
        { "key": "1", "text": "এই টীকার সম্পূর্ণ ব্যাখ্যা এখানে লিখুন।" }
      ]
    }
  ]
}
```

**মূল ফিল্ড:**
- `chapter_no` অবশ্যই unique integer হতে হবে।
- `content` এ `\n` দিয়ে অনুচ্ছেদ আলাদা করুন।
- ফাইলটি সংরক্ষণ করলে পরের বিল্ডে সব পরিবর্তন দেখা যাবে।

**ঐচ্ছিক ফিল্ড (না দিলেও চলবে):**

| ফিল্ড | কোথায় দেখাবে | উদাহরণ |
|---|---|---|
| `subtitle` | Home card-এ ও Reading স্ক্রিনের হেডারে, শিরোনামের নিচে | `"নবুওয়াত-পূর্ব আরবের একটি রূপরেখা"` |
| `page_range` | Reading স্ক্রিনের একদম উপরে, ছোট ব্যাজ আকারে | `"৩৫–৪২"` → দেখাবে "পাতা ৩৫–৪২" |
| `footnotes` | `content`-এর ভিতরে বসানো `{{note:KEY}}` মার্কার ক্লিক করলে popup-এ | নিচে দেখুন |

**টীকা (footnote) যেভাবে কাজ করে:**
1. `content`-এর যেখানে টীকা দরকার, সেখানে `{{note:KEY}}` বসান (KEY যেকোনো unique নাম/সংখ্যা হতে পারে, যেমন `{{note:1}}` বা `{{note:hadith-ref}}`)।
2. একই chapter-এর `footnotes` array-তে সেই `key` মিলিয়ে `text` লিখুন।
3. অ্যাপে মার্কারটি ছোট superscript নম্বর `[১]` আকারে দেখাবে (chapter-জুড়ে ক্রমানুসারে নম্বর হয়), ট্যাপ করলে টীকার টেক্সট popup-এ দেখাবে।
4. কোনো `key`-এর জন্য মিলানো `footnotes` এন্ট্রি না পেলে popup-এ "এই টীকার তথ্য পাওয়া যায়নি" দেখাবে — অ্যাপ ক্র্যাশ করবে না।

---

## 🚀 GitHub Actions দিয়ে APK বিল্ড

### প্রথমবার সেটআপ

```bash
# ১. GitHub-এ repository তৈরি করুন
# ২. প্রজেক্ট push করুন
git init
git add .
git commit -m "Initial commit: Dynamic Book Reader"
git remote add origin https://github.com/আপনার-username/DynamicBookReader.git
git push -u origin main
```

### APK ডাউনলোড করবেন কীভাবে?

1. GitHub-এ **Actions** ট্যাবে যান।
2. সর্বশেষ workflow run-এ ক্লিক করুন।
3. পৃষ্ঠার নিচে **Artifacts** সেকশনে `DynamicBookReader-debug-N` ডাউনলোড করুন।

---

## 🛠️ লোকাল ডেভেলপমেন্ট (Termux)

```bash
# Android SDK পাথ সেট করুন
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Debug APK বিল্ড করুন
./gradlew assembleDebug

# APK এর লোকেশন:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🏗️ টেকনিক্যাল স্ট্যাক

| লেয়ার | টেকনোলজি |
|---|---|
| **Language** | Kotlin 2.0 |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Repository Pattern |
| **JSON Parsing** | Kotlinx Serialization |
| **Navigation** | Jetpack Navigation Compose |
| **Async** | Kotlin Coroutines + StateFlow |
| **Persistence** | Jetpack DataStore Preferences |
| **CI/CD** | GitHub Actions |
| **Min SDK** | API 26 (Android 8.0) |
| **Target SDK** | API 35 (Android 15) |

---

## 📱 স্ক্রিনসমূহ

### ১. Splash Screen
- বইয়ের নাম JSON থেকে dynamically দেখায়
- Fade + scale animation
- ডেটা লোড না হওয়া পর্যন্ত অপেক্ষা করে

### ২. Home Screen (Chapter List)
- Hero banner-এ বইয়ের নাম
- Staggered animated chapter cards
- প্রতিটি card-এ chapter number badge + title + content preview

### ৩. Reading Screen
- Tap to show/hide controls (immersive mode)
- **A+ / A−**: ফন্ট সাইজ পরিবর্তন (12sp–28sp)
- **+ / −**: লাইন উচ্চতা পরিবর্তন (1.2×–2.8×)
- **☀️ দিন / 🌙 রাত / 📜 সেপিয়া**: থিম পরিবর্তন
- Scroll progress indicator
- সব পছন্দ DataStore-এ সংরক্ষিত থাকে

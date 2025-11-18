# 🎓 EduRise – Digital Learning Platform

**EduRise** is a modern **Android-based digital learning platform** that connects schools, teachers, and students in one unified ecosystem. Built with **Kotlin** and powered by **Firebase**, it offers separate dashboards for Admins, Teachers, and Students with real-time synchronization.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://developer.android.com)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase)](https://firebase.google.com)

## 🚀 Features

### 👩‍💼 Admin Dashboard
- Register schools & manage staff/students
- Create classes, sections, subjects & timetables
- Upload global content & announcements
- View school-wide analytics

### 👨‍🏫 Teacher Dashboard
- Manage assigned classes & subjects
- Upload study materials (PDF, Video, Quiz, PPT)
- Mark attendance & grade assessments
- Send notifications to students/parents

### 👨‍🎓 Student Dashboard
- View daily timetable & upcoming classes
- Access learning materials & quizzes
- Track personal progress and scores
- Receive real-time notifications

## 🧱 Tech Stack

| Component              | Technology Used                  |
|------------------------|----------------------------------|
| Frontend (App)         | Android (Kotlin)                 |
| Backend Database       | Firebase Firestore               |
| Authentication         | Firebase Auth (Email/Password)   |
| File Storage           | Firebase Storage                 |
| UI Design              | XML Layouts + Material Design    |
| IDE                    | Android Studio (Gradle)          |

## 🗂️ Firestore Database Schema

### 🏫 `schools`
| Field          | Type      | Description                     |
|---------------|-----------|---------------------------------|
| school_id     | TEXT (PK) | UUID                            |
| school_name   | TEXT      | Name of the school              |
| school_code   | TEXT      | Unique login code               |
| address       | TEXT      | School address                  |
| created_at    | TIMESTAMP | Registration date               |

### 👩‍🏫 `staff` (teachers + admins)
| Field             | Type      | Description                     |
|-------------------|-----------|---------------------------------|
| staff_id          | TEXT (PK) | Unique ID                       |
| school_id         | TEXT (FK) |                                 |
| name              | TEXT      | Full name                       |
| email             | TEXT      | Unique (Firebase Auth)          |
| role              | TEXT      | `teacher` / `admin`             |
| phone             | TEXT      |                                 |
| language_preference| TEXT     | `en` / `hi` / `pa` etc.         |
| created_at        | TIMESTAMP |                                 |

### 👨‍🎓 `students`
| Field             | Type      | Description                     |
|-------------------|-----------|---------------------------------|
| student_id        | TEXT (PK) | Unique ID                       |
| name              | TEXT      | Full name                       |
| email             | TEXT      | Unique                          |
| class_id          | TEXT (FK) |                                 |
| section           | TEXT      | A, B, C etc.                    |
| phone             | TEXT      |                                 |
| language_preference| TEXT     |                                 |
| created_at        | TIMESTAMP |                                 |

### 🏫 `classes` | `subjects` | `classAssignments` | `timetables` | `content` | `progress` | `attendance` | `analytics`
*(Full schema with all collections and fields is implemented as shown in project documentation)*

## 🧩 App Architecture
- **Firebase Firestore** – Real-time data sync
- **Android Navigation Component** – Smooth fragment transitions
- **Fragments** – Modular dashboard screens
- **MVVM Pattern** (recommended in latest code)
- **Material Design 3** – Modern UI/UX

## 📱 Screenshots

### Splash Screen & Login Screen

![WhatsApp Image 2025-11-10 at 10 39 25 AM](https://github.com/user-attachments/assets/c2de3f66-c33f-43ea-8ebe-7ef4dca6eff1)  ![WhatsApp Image 2025-11-10 at 10 39 27 AM](https://github.com/user-attachments/assets/fdd92e8f-091d-41cd-908c-8037eb1c2fff)

### Admin Dashboard Screens

![WhatsApp Image 2025-11-10 at 10 50 41 AM](https://github.com/user-attachments/assets/a9ec0563-e169-495c-ae02-36ed2b5f2d9f)  ![WhatsApp Image 2025-11-10 at 10 50 42 AM](https://github.com/user-attachments/assets/c7baaaf3-646b-4018-bdbf-e7a4bea54835)

### Teacher Dashboard Screens

![WhatsApp Image 2025-11-10 at 10 39 26 AM](https://github.com/user-attachments/assets/c8561475-f802-457a-ad6b-1e8f10530ece) ![WhatsApp Image 2025-11-10 at 10 39 26 AM (3)](https://github.com/user-attachments/assets/c6832a7d-2a06-4820-8f62-aa81c1331480) <img width="286" height="362" alt="image" src="https://github.com/user-attachments/assets/ffdbaf8f-18d5-49a8-b0fb-5242b1bf2873" />


### Student Dashboard Screens

![WhatsApp Image 2025-11-10 at 10 39 26 AM (4)](https://github.com/user-attachments/assets/7881e05e-6924-41cc-b483-3895e9b6439c)  ![WhatsApp Image 2025-11-10 at 10 39 26 AM (1)](https://github.com/user-attachments/assets/7fb11265-fc3c-4e1d-8a45-9f42dfac9085) <img width="303" height="344" alt="image" src="https://github.com/user-attachments/assets/1484e01c-a8c3-407c-a9be-f695ce22db7d" />









## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana or later
- Firebase project with Firestore, Auth & Storage enabled

### Setup
1. Clone the repository
   ```bash
   git clone https://github.com/Sharanya-Vemula/EduRise-Digital-Learning-Platform.git
   ```
2. Open in Android Studio
3. Add your google-services.json from Firebase console
4. Update Firebase Security Rules (provided in /firestore.rules)
5. Build & run!

## 🔒 Security Rules & Indexes
See firestore.rules and firestore.indexes.json

package com.example.digitallearningplatform

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class EduRiseDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "EduRiseDB.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE school(
                school_id TEXT PRIMARY KEY,
                school_name TEXT,
                school_code TEXT UNIQUE,
                address TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        db.execSQL("""
            CREATE TABLE staff(
                staff_id TEXT PRIMARY KEY,
                school_id TEXT,
                name TEXT,
                email TEXT UNIQUE,
                password_hash TEXT,
                role TEXT,
                phone TEXT,
                language_preference TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(school_id) REFERENCES school(school_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE students(
                student_id TEXT PRIMARY KEY,
                school_id TEXT,
                name TEXT,
                email TEXT UNIQUE,
                password_hash TEXT,
                class_id TEXT,
                section TEXT,
                phone TEXT,
                language_preference TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(school_id) REFERENCES school(school_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE classes(
                class_id TEXT PRIMARY KEY,
                school_id TEXT,
                class_name TEXT,
                section_count INTEGER,
                FOREIGN KEY(school_id) REFERENCES school(school_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE subjects(
                subject_id TEXT PRIMARY KEY,
                school_id TEXT,
                subject_name TEXT,
                FOREIGN KEY(school_id) REFERENCES school(school_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE classAssignments(
                assignment_id TEXT PRIMARY KEY,
                class_id TEXT,
                section TEXT,
                subject_id TEXT,
                staff_id TEXT,
                assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(class_id) REFERENCES classes(class_id),
                FOREIGN KEY(subject_id) REFERENCES subjects(subject_id),
                FOREIGN KEY(staff_id) REFERENCES staff(staff_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE timetables(
                timetable_id TEXT PRIMARY KEY,
                school_id TEXT,
                class_id TEXT,
                section TEXT,
                subject_id TEXT,
                staff_id TEXT,
                day_of_week TEXT,
                start_time TEXT,
                end_time TEXT,
                lecture_type TEXT,
                room TEXT,
                FOREIGN KEY(school_id) REFERENCES school(school_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE content(
                content_id TEXT PRIMARY KEY,
                school_id TEXT,
                class_id TEXT,
                section TEXT,
                subject_id TEXT,
                staff_id TEXT,
                title TEXT,
                type TEXT,
                file_url TEXT,
                language TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(school_id) REFERENCES school(school_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE progress(
                progress_id TEXT PRIMARY KEY,
                student_id TEXT,
                content_id TEXT,
                status TEXT,
                score FLOAT,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(student_id) REFERENCES students(student_id),
                FOREIGN KEY(content_id) REFERENCES content(content_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE attendance(
                attendance_id TEXT PRIMARY KEY,
                school_id TEXT,
                class_id TEXT,
                section TEXT,
                date DATE,
                marked_by TEXT,
                attendance_data TEXT,
                FOREIGN KEY(school_id) REFERENCES school(school_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE analytics(
                analytics_id TEXT PRIMARY KEY,
                school_id TEXT,
                metric_name TEXT,
                metric_value FLOAT,
                period TEXT,
                generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(school_id) REFERENCES school(school_id)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val tables = arrayOf(
            "school", "staff", "students", "classes", "subjects",
            "classAssignments", "timetables", "content", "progress", "attendance", "analytics"
        )
        for (table in tables) {
            db.execSQL("DROP TABLE IF EXISTS $table")
        }
        onCreate(db)
    }
}

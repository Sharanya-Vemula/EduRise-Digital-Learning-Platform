package com.example.digitallearningplatform

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentDashboard : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var schoolId: String
    private lateinit var studentId: String
    private lateinit var classId: String
    private lateinit var section: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        bottomNav = findViewById(R.id.bottomNav)

        // Get IDs passed from login
        schoolId = intent.getStringExtra("school_id") ?: ""
        studentId = intent.getStringExtra("user_id") ?: ""
        classId = intent.getStringExtra("class_id")?:""
        section= intent.getStringExtra("section")?:""

        // Default → Home Fragment
        replaceFragment(StudentHomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(StudentHomeFragment())
                R.id.nav_timetable -> replaceFragment(StudentTimetableFragment())
                R.id.nav_learning -> replaceFragment(LearningFragment())
                R.id.nav_notifications -> replaceFragment(StudentNotificationsFragment())
                R.id.nav_profile -> replaceFragment(StudentProfileFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        fragment.arguments = Bundle().apply {
            putString("school_id", schoolId)
            putString("user_id", studentId)
            putString("class_id", classId)
            putString("section", section)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

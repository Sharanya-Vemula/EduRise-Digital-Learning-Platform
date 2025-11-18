package com.example.digitallearningplatform

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminDashboard : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var schoolId: String
    private lateinit var userId: String
    private lateinit var userName: String
    private lateinit var role: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        bottomNav = findViewById(R.id.bottomNav)

        // Get all values passed from LoginActivity
        schoolId = intent.getStringExtra("school_id") ?: ""
        userId = intent.getStringExtra("user_id") ?: ""
        userName = intent.getStringExtra("name") ?: ""
        role = intent.getStringExtra("role") ?: ""

        // Default fragment → Add Class/Subject
        replaceFragment(AdminHomeFragment())

        // Bottom navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home-> replaceFragment(AdminHomeFragment())
                R.id.nav_add -> replaceFragment(AddDataFragment())
                R.id.nav_assign -> replaceFragment(ClassAssignmentFragment())
                R.id.create_tt -> replaceFragment(AdminTimetableFragment())
                R.id.announce -> replaceFragment(AdminAnnouncementsFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val bundle = Bundle()
        bundle.putString("school_id", schoolId)
        bundle.putString("user_id", userId)
        bundle.putString("name", userName)
        bundle.putString("role", role)
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

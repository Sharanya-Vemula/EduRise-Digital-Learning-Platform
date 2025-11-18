package com.example.digitallearningplatform

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class TeacherDashboard : AppCompatActivity() {

    private var schoolId: String? = null
    private var userId: String? = null
    private var email: String? = null
    private var role: String? = null
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        // Get data from login
        schoolId = intent.getStringExtra("school_id")
        userId = intent.getStringExtra("user_id")
        email = intent.getStringExtra("email")
        role = intent.getStringExtra("role")
        userName = intent.getStringExtra("name")

        // Top toolbar
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_notifications -> {
                    val not_frag = TeacherNotificationFragment()
                    not_frag.arguments=getCommonBundle()
                    loadFragment(not_frag)
                    true
                }
                R.id.action_profile -> {
                    // Open TeacherProfileFragment
                    val profileFragment = TeacherProfileFragment()
                    profileFragment.arguments = getCommonBundle()
                    loadFragment(profileFragment)
                    true
                }
                else -> false
            }
        }

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Load default fragment (Dashboard)
        val dashboardFragment = DashboardFragment()
        dashboardFragment.arguments = getCommonBundle()
        loadFragment(dashboardFragment)

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> DashboardFragment()
                R.id.nav_classes -> ClassManagementFragment()
                R.id.nav_timetable -> TeacherTimetableFragment()
                R.id.nav_attendance -> AttendanceMarkFragment()
                R.id.nav_content -> ContentUploadFragment()
                R.id.nav_progress -> ScoreAssignmentsFragment()
                else -> null
            }
            fragment?.let {
                it.arguments = getCommonBundle()
                loadFragment(it)
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Common bundle for all fragments
    private fun getCommonBundle(): Bundle {
        return Bundle().apply {
            putString("school_id", schoolId)
            putString("user_id", userId)
            putString("email", email)
            putString("role", role)
            putString("name", userName)
        }
    }
}

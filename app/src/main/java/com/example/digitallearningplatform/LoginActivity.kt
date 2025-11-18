package com.example.digitallearningplatform

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: EduRiseDatabaseHelper
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = EduRiseDatabaseHelper(this)
        firestore = FirebaseFirestore.getInstance()

        val email = findViewById<EditText>(R.id.loginEmail)
        val password = findViewById<EditText>(R.id.loginPassword)
        val schoolCode = findViewById<EditText>(R.id.loginSchoolCode)
        val roleSpinner = findViewById<Spinner>(R.id.loginRole) // keep spinner
        val loginBtn = findViewById<Button>(R.id.loginButton)
        val noAccountText = findViewById<TextView>(R.id.noAccountText)

        noAccountText.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val schoolCodeText = schoolCode.text.toString().trim()
            val selectedRole = roleSpinner.selectedItem.toString().lowercase() // role selected by user

            if (emailText.isEmpty() || passwordText.isEmpty() || schoolCodeText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firestore.collection("school")
                .whereEqualTo("school_code", schoolCodeText)
                .get()
                .addOnSuccessListener { schoolDocs ->
                    if (schoolDocs.isEmpty) {
                        Toast.makeText(this, "Invalid school code", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val schoolDoc = schoolDocs.documents[0]
                    val schoolId = schoolDoc.id

                    // Check in staff collection first
                    firestore.collection("school")
                        .document(schoolId)
                        .collection("staff")
                        .whereEqualTo("email", emailText)
                        .whereEqualTo("password_hash", passwordText)
                        .get()
                        .addOnSuccessListener { staffDocs ->
                            if (!staffDocs.isEmpty) {
                                val userDoc = staffDocs.documents[0]
                                val role = userDoc.getString("role") ?: "staff"
                                val userId = userDoc.getString("staff_id") ?: ""
                                val userName = userDoc.getString("name") ?: ""

                                // Check if selected role matches Firestore role
                                if ((role == "admin" && selectedRole != "admin") || (role == "staff" && selectedRole != "staff")) {
                                    Toast.makeText(this, "Selected role does not match your account role", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                val intent = if (role == "admin") {
                                    Intent(this, AdminDashboard::class.java)
                                } else {
                                    Intent(this, TeacherDashboard::class.java)
                                }

                                intent.putExtra("school_id", schoolId)
                                intent.putExtra("user_id", userId)
                                intent.putExtra("role", role)
                                intent.putExtra("name", userName)
                                startActivity(intent)
                                finish()

                                syncFirestoreToSQLite(schoolId, "staff")
                            } else {
                                // Check in students collection
                                firestore.collection("school")
                                    .document(schoolId)
                                    .collection("students")
                                    .whereEqualTo("email", emailText)
                                    .whereEqualTo("password_hash", passwordText)
                                    .get()
                                    .addOnSuccessListener { studentDocs ->
                                        if (!studentDocs.isEmpty) {
                                            val userDoc = studentDocs.documents[0]
                                            val userId = userDoc.getString("student_id") ?: ""
                                            val userName = userDoc.getString("name") ?: ""
                                            val classId = userDoc.getString("class_id") ?: ""
                                            val section = userDoc.getString("section") ?: ""

                                            if (selectedRole != "student") {
                                                Toast.makeText(this, "Selected role does not match your account role", Toast.LENGTH_SHORT).show()
                                                return@addOnSuccessListener
                                            }

                                            val intent = Intent(this, StudentDashboard::class.java)
                                            intent.putExtra("school_id", schoolId)
                                            intent.putExtra("user_id", userId)
                                            intent.putExtra("role", "student")
                                            intent.putExtra("name", userName)
                                            intent.putExtra("class_id", classId)
                                            intent.putExtra("section", section)
                                            startActivity(intent)
                                            finish()

                                            syncFirestoreToSQLite(schoolId, "student")
                                        } else {
                                            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error connecting to Firestore", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun syncFirestoreToSQLite(schoolId: String, role: String) {
        val db = dbHelper.writableDatabase
        val collection = if (role == "student") "students" else "staff"

        firestore.collection("school")
            .document(schoolId)
            .collection(collection)
            .get()
            .addOnSuccessListener { docs ->
                db.beginTransaction()
                try {
                    for (doc in docs) {
                        val id = doc.getString("${if (role == "student") "student_id" else "staff_id"}") ?: continue
                        val name = doc.getString("name") ?: ""
                        val email = doc.getString("email") ?: ""
                        val password = doc.getString("password_hash") ?: ""
                        val language = doc.getString("language_preference") ?: ""

                        db.execSQL(
                            """
                            INSERT OR REPLACE INTO $collection 
                            (${if (role == "student") "student_id" else "staff_id"}, school_id, name, email, password_hash, language_preference)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """.trimIndent(), arrayOf(id, schoolId, name, email, password, language)
                        )
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
    }
}

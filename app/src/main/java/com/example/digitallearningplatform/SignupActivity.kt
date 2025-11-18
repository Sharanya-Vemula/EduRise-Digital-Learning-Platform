package com.example.digitallearningplatform

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SignupActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerSection: Spinner
    private var classIds: List<String> = emptyList() // Track class IDs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        firestore = FirebaseFirestore.getInstance()

        val name = findViewById<EditText>(R.id.signupName)
        val email = findViewById<EditText>(R.id.signupEmail)
        val password = findViewById<EditText>(R.id.signupPassword)
        val roleSpinner = findViewById<Spinner>(R.id.signupRole)
        val schoolName = findViewById<EditText>(R.id.signupSchoolName)
        val schoolCode = findViewById<EditText>(R.id.signupSchoolCode)
        val signupBtn = findViewById<Button>(R.id.signupButton)
        val alreadyAccountText = findViewById<TextView>(R.id.alreadyAccountText)
        val btnCheckSchoolCode = findViewById<Button>(R.id.btnCheckSchoolCode)

        spinnerClass = findViewById(R.id.spinnerClass)
        spinnerSection = findViewById(R.id.spinnerSection)

        spinnerClass.visibility = android.view.View.GONE
        spinnerSection.visibility = android.view.View.GONE

        alreadyAccountText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Show/hide spinners for students
        roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val role = roleSpinner.selectedItem.toString().lowercase(Locale.ROOT)
                if (role == "student") {
                    spinnerClass.visibility = android.view.View.VISIBLE
                    spinnerSection.visibility = android.view.View.VISIBLE
                } else {
                    spinnerClass.visibility = android.view.View.GONE
                    spinnerSection.visibility = android.view.View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Validate school code & load classes
        btnCheckSchoolCode.setOnClickListener {
            val code = schoolCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter school code first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firestore.collection("school").document(code)
                .get()
                .addOnSuccessListener { doc ->
                    if (!doc.exists()) {
                        Toast.makeText(this, "Invalid school code!", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    loadClasses(code)
                    Toast.makeText(this, "School verified! Select class & section.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error verifying school: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        signupBtn.setOnClickListener {
            val role = roleSpinner.selectedItem.toString().lowercase(Locale.ROOT)
            val codeText = schoolCode.text.toString().trim()
            val nameText = name.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val schoolNameText = schoolName.text.toString().trim()

            if (nameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty() || codeText.isEmpty()) {
                Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = UUID.randomUUID().toString()
            val schoolRef = firestore.collection("school").document(codeText)

            // Check if email already exists
            schoolRef.collection("students").whereEqualTo("email", emailText).get()
                .addOnSuccessListener { stuSnap ->
                    schoolRef.collection("staff").whereEqualTo("email", emailText).get()
                        .addOnSuccessListener { staffSnap ->
                            if (!stuSnap.isEmpty || !staffSnap.isEmpty) {
                                Toast.makeText(this, "Email already exists!", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            val values = ContentValues().apply {
                                put("name", nameText)
                                put("email", emailText)
                                put("password_hash", passwordText)
                                put("role", role)
                                put("created_at", getLocalDateTime())
                            }

                            when (role) {
                                "admin" -> {
                                    if (schoolNameText.isEmpty()) {
                                        Toast.makeText(this, "Enter school name for admin signup", Toast.LENGTH_SHORT).show()
                                        return@addOnSuccessListener
                                    }
                                    val schoolId = UUID.randomUUID().toString()
                                    values.put("staff_id", userId)
                                    val schoolData = mapOf(
                                        "school_id" to schoolId,
                                        "school_name" to schoolNameText,
                                        "school_code" to codeText,
                                        "created_at" to getLocalDateTime()
                                    )
                                    schoolRef.set(schoolData)
                                        .addOnSuccessListener {
                                            schoolRef.collection("staff").document(userId).set(values.toMap())
                                                .addOnSuccessListener {
                                                    Toast.makeText(this, "Admin signup successful!", Toast.LENGTH_SHORT).show()
                                                    startActivity(Intent(this, LoginActivity::class.java))
                                                    finish()
                                                }
                                        }
                                }
                                "teacher" -> {
                                    values.put("staff_id", userId)
                                    schoolRef.collection("staff").document(userId).set(values.toMap())
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Teacher signup successful!", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, LoginActivity::class.java))
                                            finish()
                                        }
                                }
                                "student" -> {
                                    if (spinnerClass.selectedItem == null || spinnerSection.selectedItem == null) {
                                        Toast.makeText(this, "Select class and section", Toast.LENGTH_SHORT).show()
                                        return@addOnSuccessListener
                                    }
                                    val classId = classIds[spinnerClass.selectedItemPosition]
                                    val section = spinnerSection.selectedItem.toString()
                                    values.put("student_id", userId)
                                    values.put("class_id", classId)
                                    values.put("section", section)

                                    schoolRef.collection("students").document(userId).set(values.toMap())
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Student signup successful!", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, LoginActivity::class.java))
                                            finish()
                                        }
                                }
                            }

                        }
                }
        }
    }

    private fun loadClasses(schoolCode: String) {
        firestore.collection("school").document(schoolCode).collection("classes")
            .get()
            .addOnSuccessListener { snapshot ->
                val classList = snapshot.documents.map { it.getString("class_name") ?: "" }
                classIds = snapshot.documents.map { it.getString("class_id") ?: "" }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, classList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerClass.adapter = adapter

                spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                        val sections = snapshot.documents[position].get("sections") as? List<String> ?: emptyList()
                        val sectionAdapter = ArrayAdapter(this@SignupActivity, android.R.layout.simple_spinner_item, sections)
                        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerSection.adapter = sectionAdapter
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
    }

    private fun getLocalDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun ContentValues.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        this.valueSet().forEach { if (it.value != null) map[it.key] = it.value!! }
        return map
    }
}

package com.example.digitallearningplatform


import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class StudentProfileFragment : Fragment() {

    private lateinit var db: FirebaseFirestore

    private lateinit var nameField: EditText
    private lateinit var emailField: EditText
    private lateinit var phoneField: EditText
    private lateinit var languageField: Spinner
    private lateinit var passwordField: EditText
    private lateinit var saveButton: Button
    private lateinit var statusText: TextView

    private lateinit var schoolId: String
    private lateinit var studentId: String
    private lateinit var classId: String
    private lateinit var section: String

    private val languageOptions = listOf("English", "Hindi", "Punjabi", "Tamil", "Telugu")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_student_profile, container, false)

        db = FirebaseFirestore.getInstance()

        // Initialize views
        nameField = view.findViewById(R.id.studentNameField)
        emailField = view.findViewById(R.id.studentEmailField)
        phoneField = view.findViewById(R.id.studentPhoneField)
        languageField = view.findViewById(R.id.languageSpinner)
        passwordField = view.findViewById(R.id.studentPasswordField)
        saveButton = view.findViewById(R.id.saveButton)
        statusText = view.findViewById(R.id.statusMessage)

        // Setup language spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languageOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageField.adapter = adapter

        // Retrieve passed arguments
        schoolId = arguments?.getString("school_id") ?: ""
        studentId = arguments?.getString("user_id") ?: ""
        classId = arguments?.getString("class_id") ?: ""
        section = arguments?.getString("section") ?: ""

        if (schoolId.isEmpty() || studentId.isEmpty()) {
            statusText.text = "Missing student or school info"
            return view
        }

        lifecycleScope.launch { loadStudentProfile() }

        saveButton.setOnClickListener { updateProfile() }

        return view
    }

    /** Fetch student profile details **/
    private suspend fun loadStudentProfile() {
        try {
            val studentDoc = db.collection("school").document(schoolId)
                .collection("students").document(studentId)
                .get().await()

            if (studentDoc.exists()) {
                nameField.setText(studentDoc.getString("name") ?: "")
                emailField.setText(studentDoc.getString("email") ?: "")
                phoneField.setText(studentDoc.getString("phone") ?: "")
                passwordField.setText(studentDoc.getString("password_hash") ?: "")

                val lang = studentDoc.getString("language_preference") ?: "English"
                val pos = languageOptions.indexOf(lang)
                if (pos >= 0) languageField.setSelection(pos)
            } else {
                statusText.text = "Student profile not found."
            }

        } catch (e: Exception) {
            Log.e("FirestoreError", "Error fetching profile", e)
            statusText.text = "Error fetching profile: ${e.message}"
        }
    }

    /** Update student profile details **/
    private fun updateProfile() {
        val name = nameField.text.toString().trim()
        val email = emailField.text.toString().trim()
        val phone = phoneField.text.toString().trim()
        val password = passwordField.text.toString().trim()
        val language = languageField.selectedItem.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Name, Email, and Password are required.", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedData = hashMapOf(
            "name" to name,
            "email" to email,
            "phone" to phone,
            "language_preference" to language,
            "password_hash" to password,
            "updated_at" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )

        db.collection("school").document(schoolId)
            .collection("students").document(studentId)
            .update(updatedData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

package com.example.digitallearningplatform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class TeacherProfileFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var schoolId: String
    private lateinit var userId: String
    private lateinit var role: String
    private lateinit var userName: String

    private lateinit var etTeacherName: EditText
    private lateinit var etTeacherEmail: EditText
    private lateinit var etTeacherPhone: EditText
    private lateinit var spLanguagePref: Spinner
    private lateinit var etTeacherPassword: EditText
    private lateinit var btnSaveTeacherProfile: Button

    private val languages = listOf("English", "Hindi", "Punjabi") // Example languages

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teacher_profile, container, false)

        db = FirebaseFirestore.getInstance()
        schoolId = arguments?.getString("school_id") ?: ""
        userId = arguments?.getString("user_id") ?: ""
        role = arguments?.getString("role") ?: ""
        userName = arguments?.getString("name") ?: ""

        etTeacherName = view.findViewById(R.id.etTeacherName)
        etTeacherEmail = view.findViewById(R.id.etTeacherEmail)
        etTeacherPhone = view.findViewById(R.id.etTeacherPhone)
        spLanguagePref = view.findViewById(R.id.spLanguagePref)
        etTeacherPassword = view.findViewById(R.id.etTeacherPassword)
        btnSaveTeacherProfile = view.findViewById(R.id.btnSaveTeacherProfile)

        // Setup Spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spLanguagePref.adapter = adapter

        // Autofill name from login
        etTeacherName.setText(userName)

        // Load full profile from Firebase
        loadTeacherProfile()

        btnSaveTeacherProfile.setOnClickListener {
            saveTeacherProfile()
        }

        return view
    }

    private fun loadTeacherProfile() {
        if (schoolId.isEmpty() || userId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid user data", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("school").document(schoolId)
            .collection("staff").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etTeacherName.setText(doc.getString("name") ?: "")
                    etTeacherEmail.setText(doc.getString("email") ?: "")
                    etTeacherPhone.setText(doc.getString("phone") ?: "")
                    etTeacherPassword.setText(doc.getString("password_hash") ?: "")

                    // Set Spinner selection based on Firebase value
                    val lang = doc.getString("language_preference") ?: "English"
                    val position = languages.indexOf(lang)
                    if (position >= 0) spLanguagePref.setSelection(position)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveTeacherProfile() {
        val updatedData = hashMapOf(
            "name" to etTeacherName.text.toString(),
            "email" to etTeacherEmail.text.toString(),
            "phone" to etTeacherPhone.text.toString(),
            "language_preference" to spLanguagePref.selectedItem.toString(),
            "password_hash" to etTeacherPassword.text.toString()
        )

        db.collection("school").document(schoolId)
            .collection("staff").document(userId)
            .update(updatedData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
}

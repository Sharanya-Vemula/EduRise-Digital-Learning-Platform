package com.example.digitallearningplatform


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AdminAnnouncementsFragment : Fragment() {

    private lateinit var etTitle: EditText
    private lateinit var etMessage: EditText
    private lateinit var spinnerAudience: Spinner
    private lateinit var btnSendAnnouncement: Button

    private val db = FirebaseFirestore.getInstance()
    private lateinit var schoolId: String
    private lateinit var adminId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_announcements, container, false)

        etTitle = view.findViewById(R.id.etTitle)
        etMessage = view.findViewById(R.id.etMessage)
        spinnerAudience = view.findViewById(R.id.spinnerAudience)
        btnSendAnnouncement = view.findViewById(R.id.btnSendAnnouncement)

        schoolId = arguments?.getString("school_id") ?: ""
        adminId = arguments?.getString("user_id") ?: ""

        btnSendAnnouncement.setOnClickListener {
            sendAnnouncement()
        }

        return view
    }

    private fun sendAnnouncement() {
        val title = etTitle.text.toString().trim()
        val message = etMessage.text.toString().trim()
        val targetAudience = spinnerAudience.selectedItem.toString()

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val announcementId = UUID.randomUUID().toString()

        val announcement = hashMapOf(
            "announcement_id" to announcementId,
            "title" to title,
            "message" to message,
            "target_audience" to targetAudience,
            "timestamp" to Timestamp.now(),
            "created_by" to adminId
        )

        db.collection("school").document(schoolId)
            .collection("announcements").document(announcementId)
            .set(announcement)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Announcement sent!", Toast.LENGTH_SHORT).show()
                etTitle.text.clear()
                etMessage.text.clear()
                spinnerAudience.setSelection(0)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

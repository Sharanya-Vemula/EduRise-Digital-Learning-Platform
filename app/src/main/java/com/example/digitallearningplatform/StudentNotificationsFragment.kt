package com.example.digitallearningplatform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StudentNotificationsFragment : Fragment() {

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val db = FirebaseFirestore.getInstance()

    private lateinit var schoolId: String
    private lateinit var classId: String
    private lateinit var section: String

    private val notificationList = mutableListOf<Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_student_notifications, container, false)

        notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView)
        notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = NotificationAdapter(notificationList)
        notificationsRecyclerView.adapter = adapter

        schoolId = arguments?.getString("school_id") ?: ""
        classId = arguments?.getString("class_id") ?: ""
        section = arguments?.getString("section") ?: ""

        if (schoolId.isEmpty() || classId.isEmpty() || section.isEmpty()) {
            Toast.makeText(requireContext(), "Missing class details", Toast.LENGTH_SHORT).show()
        } else {
            loadClassNameAndNotifications()
        }

        return view
    }

    private fun loadClassNameAndNotifications() {
        // Step 1: Get class name from class_id
        val classRef = db.collection("school").document(schoolId)
            .collection("classes").document(classId)

        classRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val className = doc.getString("class_name") ?: ""
                if (className.isNotEmpty()) {
                    loadNotifications(className)
                } else {
                    Toast.makeText(requireContext(), "Class name not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Invalid class record", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error loading class info", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNotifications(className: String) {
        notificationList.clear()

        val schoolRef = db.collection("school").document(schoolId)

        // Admin announcements for students or both
        val announcementsRef = schoolRef.collection("announcements")
            .whereIn("target_audience", listOf("Students", "Both"))

        // Teacher notifications → classes is an array of maps [{class: "6th", section: "A"}]
        val teacherNotifsRef = schoolRef.collection("notifications")
            .whereArrayContains("classes", mapOf("class" to className, "section" to section))

        announcementsRef.get().addOnSuccessListener { announceSnap ->
            for (doc in announceSnap.documents) {
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["source"] = "Admin"
                notificationList.add(data)
            }

            teacherNotifsRef.get().addOnSuccessListener { notifSnap ->
                for (doc in notifSnap.documents) {
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["source"] = "Teacher"
                    notificationList.add(data)
                }

                // Sort by date/time descending (timestamp or created_at)
                notificationList.sortByDescending {
                    val timestamp = it["timestamp"] as? Timestamp
                    val createdAt = it["created_at"] as? String
                    when {
                        timestamp != null -> timestamp.toDate().time
                        !createdAt.isNullOrEmpty() -> {
                            try {
                                val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                parser.parse(createdAt)?.time ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                        }
                        else -> 0L
                    }
                }

                adapter.notifyDataSetChanged()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading teacher notifications", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error loading announcements", Toast.LENGTH_SHORT).show()
        }
    }

    // -------------------- RecyclerView Adapter --------------------
    inner class NotificationAdapter(
        private val notifications: List<Map<String, Any>>
    ) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

        inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvMessage: TextView = view.findViewById(R.id.tvMessage)
            val tvSource: TextView = view.findViewById(R.id.tvSource)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view)
        }

        override fun getItemCount(): Int = notifications.size

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notif = notifications[position]
            holder.tvTitle.text = notif["title"] as? String ?: "(No Title)"
            holder.tvMessage.text = notif["message"] as? String ?: ""

            val source = notif["source"] as? String ?: "Unknown"

            // Handle both Firestore Timestamp and String date
            val timestamp = notif["timestamp"] as? Timestamp
            val createdAtString = notif["created_at"] as? String

            val formattedDate = when {
                timestamp != null -> {
                    val date = timestamp.toDate()
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
                }
                !createdAtString.isNullOrEmpty() -> {
                    try {
                        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val date = parser.parse(createdAtString)
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date!!)
                    } catch (e: Exception) {
                        ""
                    }
                }
                else -> ""
            }

            holder.tvSource.text = "From: $source  •  $formattedDate"
        }
    }
}

package com.example.digitallearningplatform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TeacherNotificationFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoAnnouncements: TextView
    private val announcementsList = mutableListOf<Announcement>()
    private lateinit var schoolId:String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teacher_notification, container, false)

        db = FirebaseFirestore.getInstance()
        schoolId = arguments?.getString("school_id") ?: ""
        if (schoolId.isEmpty()) {
            Toast.makeText(requireContext(), "School ID not found", Toast.LENGTH_SHORT).show()
            return view
        }

        recyclerView = view.findViewById(R.id.rvAnnouncements)
        progressBar = view.findViewById(R.id.progressBar)
        tvNoAnnouncements = view.findViewById(R.id.tvNoAnnouncements)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = AnnouncementAdapter(announcementsList)

        loadAnnouncements()

        return view
    }

    private fun loadAnnouncements() {
        progressBar.visibility = View.VISIBLE
        db.collection("school")
            .document(schoolId)
            .collection("announcements")
            .get()
            .addOnSuccessListener { result ->
                announcementsList.clear()
                for (doc in result) {
                    val target = doc.getString("target_audience")?.trim()
                    if (target == "Teachers" || target == "Both") {
                        val announcement = Announcement(
                            doc.getString("announcement_id") ?: "",
                            doc.getString("title") ?: "",
                            doc.getString("message") ?: "",
                            doc.getTimestamp("timestamp")
                        )
                        announcementsList.add(announcement)
                    }
                }
                progressBar.visibility = View.GONE
                tvNoAnnouncements.visibility = if (announcementsList.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to load announcements", Toast.LENGTH_SHORT).show()
            }

    }

    // Data class for announcements
    data class Announcement(
        val id: String,
        val title: String,
        val message: String,
        val timestamp: Timestamp?
    )

    // ✅ Inner Adapter class
    inner class AnnouncementAdapter(private val announcements: List<Announcement>) :
        RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_announcement, parent, false)
            return AnnouncementViewHolder(view)
        }

        override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
            val announcement = announcements[position]
            holder.tvTitle.text = announcement.title
            holder.tvMessage.text = announcement.message
            holder.tvTimestamp.text = announcement.timestamp?.toDate()?.let { formatDate(it) } ?: ""
        }

        override fun getItemCount(): Int = announcements.size

        inner class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
            val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        }

        private fun formatDate(date: Date): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(date)
        }
    }
}

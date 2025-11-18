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
import com.google.firebase.firestore.FirebaseFirestore

class StudentTimetableFragment : Fragment() {

    private lateinit var daysRecyclerView: RecyclerView
    private lateinit var timetableRecyclerView: RecyclerView

    private val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    private var selectedDay = "Monday"

    private val db = FirebaseFirestore.getInstance()
    private lateinit var schoolId: String
    private lateinit var studentId: String
    private var classId: String? = null
    private var section: String? = null

    private val timetableList = mutableListOf<Map<String, Any>>()
    private lateinit var timetableAdapter: TimetableAdapter
    private lateinit var dayAdapter: DayAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_student_timetable, container, false)

        daysRecyclerView = view.findViewById(R.id.daysRecyclerView)
        timetableRecyclerView = view.findViewById(R.id.timetableRecyclerView)

        schoolId = arguments?.getString("school_id") ?: ""
        studentId = arguments?.getString("user_id") ?: ""

        setupDaysRecycler()
        setupTimetableRecycler()
        loadStudentClassInfo() // Fetch class_id & section before timetable

        return view
    }

    private fun setupDaysRecycler() {
        dayAdapter = DayAdapter(days) { day ->
            selectedDay = day
            loadTimetable(day)
        }
        daysRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        daysRecyclerView.adapter = dayAdapter
    }

    private fun setupTimetableRecycler() {
        timetableAdapter = TimetableAdapter(timetableList)
        timetableRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        timetableRecyclerView.adapter = timetableAdapter
    }

    // Fetch student's class_id & section from Firestore
    private fun loadStudentClassInfo() {
        db.collection("school").document(schoolId)
            .collection("students").document(studentId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    classId = doc.getString("class_id")
                    section = doc.getString("section")
                    if (classId != null && section != null) {
                        loadTimetable(selectedDay)
                    } else {
                        Toast.makeText(requireContext(), "Class/Section not found for student", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load student info: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTimetable(day: String) {
        if (classId == null || section == null) return

        db.collection("school").document(schoolId)
            .collection("timetables")
            .whereEqualTo("class_id", classId)
            .whereEqualTo("section", section)
            .whereEqualTo("day", day)
            .get()
            .addOnSuccessListener { snapshot ->
                timetableList.clear()
                for (doc in snapshot.documents) {
                    val periods = doc.get("periods") as? List<Map<String, Any>> ?: emptyList()
                    timetableList.addAll(periods)
                }
                timetableAdapter.notifyDataSetChanged()

                if (timetableList.isEmpty()) {
                    Toast.makeText(requireContext(), "No periods for $day", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load timetable: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------------- INNER ADAPTERS ----------------------

    inner class DayAdapter(
        private val days: List<String>,
        private val onDayClick: (String) -> Unit
    ) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

        private var selectedPosition = 0

        inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dayText: TextView = view.findViewById(R.id.dayText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.student_item_day, parent, false)
            return DayViewHolder(view)
        }

        override fun getItemCount(): Int = days.size

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            holder.dayText.text = days[position]
            holder.dayText.isSelected = selectedPosition == position
            holder.dayText.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = position
                notifyItemChanged(previous)
                notifyItemChanged(position)
                onDayClick(days[position])
            }
        }
    }

    inner class TimetableAdapter(
        private val periods: List<Map<String, Any>>
    ) : RecyclerView.Adapter<TimetableAdapter.PeriodViewHolder>() {

        inner class PeriodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val subjectText: TextView = view.findViewById(R.id.subjectText)
            val timeText: TextView = view.findViewById(R.id.timeText)
            val teacherText: TextView = view.findViewById(R.id.teacherText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeriodViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_period, parent, false)
            return PeriodViewHolder(view)
        }

        override fun getItemCount(): Int = periods.size

        override fun onBindViewHolder(holder: PeriodViewHolder, position: Int) {
            val period = periods[position]
            holder.subjectText.text = period["subject"] as? String ?: ""
            holder.timeText.text = "${period["start_time"]} - ${period["end_time"]}"
            holder.teacherText.text = "By: ${period["teacher_name"] ?: "N/A"}"
        }
    }
}

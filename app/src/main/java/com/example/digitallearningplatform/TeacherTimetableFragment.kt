package com.example.digitallearningplatform

import android.annotation.SuppressLint
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

class TeacherTimetableFragment : Fragment() {

    private lateinit var daysRecyclerView: RecyclerView
    private lateinit var timetableRecyclerView: RecyclerView

    private val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    private var selectedDay = "Monday"

    private val db = FirebaseFirestore.getInstance()
    private lateinit var schoolId: String
    private lateinit var teacherId: String

    private val timetableList = mutableListOf<Map<String, Any>>() // periods for selected day
    private lateinit var timetableAdapter: TimetableAdapter
    private lateinit var dayAdapter: DayAdapter

    // Map to hold class_id -> class_name
    private val classIdNameMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teacher_timetable, container, false)

        daysRecyclerView = view.findViewById(R.id.daysRecyclerView)
        timetableRecyclerView = view.findViewById(R.id.timetableRecyclerView)

        schoolId = arguments?.getString("school_id") ?: ""
        teacherId = arguments?.getString("user_id") ?: ""

        loadClasses()
        setupDaysRecycler()
        setupTimetableRecycler()
        loadTimetable(selectedDay)

        return view
    }

    private fun loadClasses() {
        db.collection("school").document(schoolId)
            .collection("classes")
            .get()
            .addOnSuccessListener { snapshot ->
                classIdNameMap.clear()
                for (doc in snapshot.documents) {
                    classIdNameMap[doc.id] = doc.getString("class_name") ?: doc.id
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load classes: ${it.message}", Toast.LENGTH_SHORT).show()
            }
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

    private fun loadTimetable(day: String) {
        db.collection("school").document(schoolId)
            .collection("timetables")
            .whereEqualTo("day", day)
            .get()
            .addOnSuccessListener { snapshot ->
                timetableList.clear()
                for (doc in snapshot.documents) {
                    val classId = doc.getString("class_id") ?: ""
                    val section = doc.getString("section") ?: ""
                    val periods = doc.get("periods") as? List<Map<String, Any>> ?: emptyList()
                    val teacherPeriods = periods.filter { it["teacher_id"] == teacherId }

                    teacherPeriods.forEach { period ->
                        timetableList.add(period + mapOf("class_id" to classId, "section" to section))
                    }
                }
                timetableAdapter.notifyDataSetChanged()
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
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
            return DayViewHolder(view)
        }

        override fun getItemCount(): Int = days.size

        override fun onBindViewHolder(holder: DayViewHolder, @SuppressLint("RecyclerView") position: Int) {
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
            val classSectionText: TextView = view.findViewById(R.id.classSectionText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeriodViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_period, parent, false)
            return PeriodViewHolder(view)
        }

        override fun getItemCount(): Int = periods.size

        override fun onBindViewHolder(holder: PeriodViewHolder, position: Int) {
            val period = periods[position]
            holder.subjectText.text = period["subject"] as? String ?: ""
            holder.timeText.text = "${period["start_time"]} - ${period["end_time"]}"

            val classId = period["class_id"] as? String ?: ""
            val section = period["section"] as? String ?: ""
            val className = classIdNameMap[classId] ?: classId
            holder.classSectionText.text = "$className - $section"
        }
    }
}

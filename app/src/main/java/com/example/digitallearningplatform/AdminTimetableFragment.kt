// Your imports
package com.example.digitallearningplatform

import android.app.TimePickerDialog
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class AdminTimetableFragment : Fragment() {

    private lateinit var classSpinner: Spinner
    private lateinit var sectionSpinner: Spinner
    private lateinit var daySpinner: Spinner
    private lateinit var addPeriodButton: Button
    private lateinit var saveButton: Button
    private lateinit var periodContainer: LinearLayout
    private lateinit var loadTimetableButton: Button

    private lateinit var viewTeacherSpinner: Spinner
    private lateinit var viewClassSpinner: Spinner
    private lateinit var viewSectionSpinner: Spinner
    private lateinit var viewDaySpinner: Spinner
    private lateinit var viewButton: Button
    private lateinit var viewPeriodContainer: LinearLayout

    private val db = FirebaseFirestore.getInstance()
    private lateinit var schoolId: String
    private lateinit var adminId: String

    private val subjectList = mutableListOf<String>()
    private val teacherNames = mutableListOf<String>()
    private val teacherIds = mutableListOf<String>()
    private var classIds = listOf<String>()
    private val classIdNameMap = mutableMapOf<String, String>()

    private var currentTimetableDocId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_timetable, container, false)

        classSpinner = view.findViewById(R.id.classSpinner)
        sectionSpinner = view.findViewById(R.id.sectionSpinner)
        daySpinner = view.findViewById(R.id.daySpinner)
        addPeriodButton = view.findViewById(R.id.addPeriodButton)
        saveButton = view.findViewById(R.id.saveButton)
        periodContainer = view.findViewById(R.id.periodContainer)
        loadTimetableButton = view.findViewById(R.id.loadTimetableButton)

        viewTeacherSpinner = view.findViewById(R.id.viewTeacherSpinner)
        viewClassSpinner = view.findViewById(R.id.viewClassSpinner)
        viewSectionSpinner = view.findViewById(R.id.viewSectionSpinner)
        viewDaySpinner = view.findViewById(R.id.viewDaySpinner)
        viewButton = view.findViewById(R.id.viewButton)
        viewPeriodContainer = view.findViewById(R.id.viewPeriodContainer)

        schoolId = arguments?.getString("school_id") ?: ""
        adminId = arguments?.getString("user_id") ?: ""

        loadClassesAndSections()
        loadTeachers()
        loadSubjects()
        setupDaySpinner(daySpinner)
        setupDaySpinner(viewDaySpinner)

        addPeriodButton.setOnClickListener { addPeriodRow() }
        saveButton.setOnClickListener { saveOrUpdateTimetable() }
        loadTimetableButton.setOnClickListener { loadTimetableForEditing() }
        viewButton.setOnClickListener { viewTimetable() }

        return view
    }

    // Same loadClassesAndSections(), loadTeachers(), loadSubjects(), setupDaySpinner(), addPeriodRow(), showTimePicker() as above
    private fun loadClassesAndSections() {
        db.collection("school").document(schoolId).collection("classes")
            .get()
            .addOnSuccessListener { snapshot ->
                val classList = snapshot.documents.map { it.getString("class_name") ?: "" }
                classIds = snapshot.documents.map { it.id }

                classIdNameMap.clear()
                snapshot.documents.forEach { classIdNameMap[it.id] = it.getString("class_name") ?: "" }

                val classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Select Class") + classList)
                classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                classSpinner.adapter = classAdapter

                val viewClassAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Select Class") + classList)
                viewClassAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                viewClassSpinner.adapter = viewClassAdapter

                classSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val sections = if (position > 0) {
                            snapshot.documents[position - 1].get("sections") as? List<String> ?: emptyList()
                        } else emptyList()

                        val sectionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Select Section") + sections)
                        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        sectionSpinner.adapter = sectionAdapter
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                viewClassSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val sections = if (position > 0) {
                            snapshot.documents[position - 1].get("sections") as? List<String> ?: emptyList()
                        } else emptyList()

                        val sectionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Select Section") + sections)
                        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        viewSectionSpinner.adapter = sectionAdapter
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load classes: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadTeachers() {
        db.collection("school").document(schoolId)
            .collection("staff")
            .whereEqualTo("role", "staff")
            .get()
            .addOnSuccessListener { snapshot ->
                teacherNames.clear()
                teacherIds.clear()
                teacherNames.addAll(snapshot.documents.map { it.getString("name") ?: "" })
                teacherIds.addAll(snapshot.documents.map { it.id })

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Select Teacher") + teacherNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                viewTeacherSpinner.adapter = adapter
            }
    }

    private fun loadSubjects() {
        db.collection("school").document(schoolId)
            .collection("subjects")
            .get()
            .addOnSuccessListener { snapshot ->
                subjectList.clear()
                subjectList.addAll(snapshot.documents.map { it.getString("subject_name") ?: "" })
            }
    }

    private fun setupDaySpinner(spinner: Spinner) {
        val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun addPeriodRow() {
        val rowView = layoutInflater.inflate(R.layout.period_row, null)

        val subjectSpinner = rowView.findViewById<Spinner>(R.id.subjectSpinnerRow)
        val teacherSpinner = rowView.findViewById<Spinner>(R.id.teacherSpinnerRow)
        val startTime = rowView.findViewById<EditText>(R.id.startTimeRow)
        val endTime = rowView.findViewById<EditText>(R.id.endTimeRow)
        val removeBtn = rowView.findViewById<ImageButton>(R.id.removePeriodRow)

        subjectSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subjectList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        teacherSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teacherNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        startTime.setOnClickListener { showTimePicker(startTime) }
        endTime.setOnClickListener { showTimePicker(endTime) }

        removeBtn.setOnClickListener { periodContainer.removeView(rowView) }

        periodContainer.addView(rowView)
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            editText.setText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }


    private fun loadTimetableForEditing() {
        val classIdIndex = classSpinner.selectedItemPosition
        if (classIdIndex == 0) {
            Toast.makeText(requireContext(), "Select class", Toast.LENGTH_SHORT).show()
            return
        }
        val classId = classIds[classIdIndex - 1]
        val section = sectionSpinner.selectedItem.toString()
        val day = daySpinner.selectedItem.toString()

        db.collection("school").document(schoolId).collection("timetables")
            .whereEqualTo("class_id", classId)
            .whereEqualTo("section", section)
            .whereEqualTo("day", day)
            .get()
            .addOnSuccessListener { snapshot ->
                periodContainer.removeAllViews()
                if (snapshot.documents.isNotEmpty()) {
                    val doc = snapshot.documents[0]
                    currentTimetableDocId = doc.id
                    val periods = doc.get("periods") as? List<Map<String, String>> ?: emptyList()
                    for (period in periods) {
                        val rowView = layoutInflater.inflate(R.layout.period_row, null)
                        val subjectSpinner = rowView.findViewById<Spinner>(R.id.subjectSpinnerRow)
                        val teacherSpinner = rowView.findViewById<Spinner>(R.id.teacherSpinnerRow)
                        val startTime = rowView.findViewById<EditText>(R.id.startTimeRow)
                        val endTime = rowView.findViewById<EditText>(R.id.endTimeRow)
                        val removeBtn = rowView.findViewById<ImageButton>(R.id.removePeriodRow)

                        subjectSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subjectList).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }
                        teacherSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teacherNames).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }

                        subjectSpinner.setSelection(subjectList.indexOf(period["subject"]))
                        val teacherIndex = teacherIds.indexOf(period["teacher_id"])
                        if (teacherIndex != -1) teacherSpinner.setSelection(teacherIndex)
                        startTime.setText(period["start_time"])
                        endTime.setText(period["end_time"])

                        startTime.setOnClickListener { showTimePicker(startTime) }
                        endTime.setOnClickListener { showTimePicker(endTime) }
                        removeBtn.setOnClickListener { periodContainer.removeView(rowView) }

                        periodContainer.addView(rowView)
                    }
                } else {
                    currentTimetableDocId = null
                    Toast.makeText(requireContext(), "No existing timetable. You can create one.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading timetable: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveOrUpdateTimetable() {
        val classIdIndex = classSpinner.selectedItemPosition
        if (classIdIndex == 0) {
            Toast.makeText(requireContext(), "Select class", Toast.LENGTH_SHORT).show()
            return
        }
        val classId = classIds[classIdIndex - 1]
        val section = sectionSpinner.selectedItem.toString()
        val day = daySpinner.selectedItem.toString()

        val periodsData = mutableListOf<Map<String, String>>()
        for (i in 0 until periodContainer.childCount) {
            val rowView = periodContainer.getChildAt(i)
            val subjectSpinner = rowView.findViewById<Spinner>(R.id.subjectSpinnerRow)
            val teacherSpinner = rowView.findViewById<Spinner>(R.id.teacherSpinnerRow)
            val startTime = rowView.findViewById<EditText>(R.id.startTimeRow)
            val endTime = rowView.findViewById<EditText>(R.id.endTimeRow)
            val teacherIndex = teacherSpinner.selectedItemPosition

            periodsData.add(
                mapOf(
                    "subject" to subjectList[subjectSpinner.selectedItemPosition],
                    "teacher_id" to teacherIds[teacherIndex],
                    "teacher_name" to teacherNames[teacherIndex],
                    "start_time" to startTime.text.toString(),
                    "end_time" to endTime.text.toString()
                )
            )
        }

        val timetableData = hashMapOf(
            "class_id" to classId,
            "section" to section,
            "day" to day,
            "periods" to periodsData,
            "created_by" to adminId,
            "created_at" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )

        if (currentTimetableDocId != null) {
            db.collection("school").document(schoolId)
                .collection("timetables")
                .document(currentTimetableDocId!!)
                .set(timetableData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Timetable updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            timetableData["timetable_id"] = UUID.randomUUID().toString()
            db.collection("school").document(schoolId)
                .collection("timetables")
                .add(timetableData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Timetable saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Keep your viewTimetable() as it is
    private fun viewTimetable() {
        val selectedTeacherIndex = viewTeacherSpinner.selectedItemPosition
        val selectedTeacherId = if (selectedTeacherIndex > 0) teacherIds[selectedTeacherIndex - 1] else null

        val selectedClassIndex = viewClassSpinner.selectedItemPosition
        val selectedClassId = if (selectedClassIndex > 0) classIds[selectedClassIndex - 1] else null
        val selectedSection = if (selectedClassIndex > 0) viewSectionSpinner.selectedItem.toString() else null
        val selectedDay = viewDaySpinner.selectedItem.toString()

        if ((selectedTeacherId != null && (selectedClassId != null || selectedSection != null)) ||
            (selectedTeacherId == null && selectedClassId == null)) {
            Toast.makeText(requireContext(), "Select either Teacher + Day or Class + Section + Day", Toast.LENGTH_SHORT).show()
            return
        }

        val baseRef = db.collection("school").document(schoolId).collection("timetables")
        val query: Query = if (selectedTeacherId != null) {
            // Teacher + Day
            baseRef.whereEqualTo("day", selectedDay)
        } else if (selectedClassId != null && selectedSection != null) {
            // Class + Section + Day
            baseRef.whereEqualTo("class_id", selectedClassId)
                .whereEqualTo("section", selectedSection)
                .whereEqualTo("day", selectedDay)
        } else baseRef

        viewPeriodContainer.removeAllViews()

        query.get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val classId = doc.getString("class_id") ?: ""
                val className = classIdNameMap[classId] ?: classId
                val section = doc.getString("section") ?: ""
                val day = doc.getString("day") ?: ""
                val periods = doc.get("periods") as? List<Map<String, String>> ?: emptyList()

                // Filter periods if teacher is selected
                val filteredPeriods = if (selectedTeacherId != null) {
                    periods.filter { it["teacher_id"] == selectedTeacherId }
                } else periods

                if (filteredPeriods.isEmpty()) continue

                val header = TextView(requireContext())
                header.text = "Class: $className | Section: $section | Day: $day"
                header.textSize = 16f
                header.setPadding(0, 16, 0, 8)
                viewPeriodContainer.addView(header)

                for (period in filteredPeriods) {
                    val tv = TextView(requireContext())
                    tv.text = "${period["start_time"]} - ${period["end_time"]}: ${period["subject"]} (${period["teacher_name"]})"
                    tv.setPadding(16, 8, 16, 8)
                    viewPeriodContainer.addView(tv)
                }
            }

            if (viewPeriodContainer.childCount == 0) {
                val tv = TextView(requireContext())
                tv.text = "No timetable found for selected criteria."
                tv.setPadding(16, 8, 16, 8)
                viewPeriodContainer.addView(tv)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error fetching timetable: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

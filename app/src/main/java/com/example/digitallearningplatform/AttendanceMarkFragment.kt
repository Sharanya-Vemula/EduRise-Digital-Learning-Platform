package com.example.digitallearningplatform

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AttendanceMarkFragment : Fragment() {

    private lateinit var classSpinner: Spinner
    private lateinit var dateText: TextView
    private lateinit var studentListView: ListView
    private lateinit var submitButton: Button

    private lateinit var schoolId: String
    private lateinit var userId: String
    private val db = FirebaseFirestore.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val assignedList = mutableListOf<AssignedItem>()
    private var selectedDate: String = ""
    private var currentStudents = mutableListOf<StudentItem>()
    private lateinit var adapter: StudentAdapter

    data class AssignedItem(val classId: String, val className: String, val section: String)
    data class StudentItem(val stuId: String, val name: String, var isPresent: Boolean = true)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_attendance_mark, container, false)

        classSpinner = view.findViewById(R.id.classSpinner)
        dateText = view.findViewById(R.id.dateText)
        studentListView = view.findViewById(R.id.studentListView)
        submitButton = view.findViewById(R.id.submitButton)

        schoolId = arguments?.getString("school_id") ?: ""
        userId = arguments?.getString("user_id") ?: ""

        if (schoolId.isEmpty() || userId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing user or school info", Toast.LENGTH_SHORT).show()
            return view
        }

        loadAssignedClasses()
        setupDatePicker()

        submitButton.setOnClickListener { saveAttendance() }

        return view
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        dateText.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = dateFormat.format(calendar.time)
                dateText.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun loadAssignedClasses() {
        coroutineScope.launch {
            try {
                val assignmentSnap = db.collection("school").document(schoolId)
                    .collection("classAssignments")
                    .whereEqualTo("staff_id", userId)
                    .get()
                    .await()

                if (assignmentSnap.isEmpty) {
                    Toast.makeText(requireContext(), "No assigned classes found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val options = mutableListOf<String>()
                assignedList.clear()

                for (doc in assignmentSnap) {
                    val classId = doc.getString("class_id") ?: continue
                    val section = doc.getString("section") ?: "N/A"
                    val classDoc = db.collection("school").document(schoolId)
                        .collection("classes").document(classId)
                        .get().await()
                    val className = classDoc.getString("class_name") ?: "Unknown Class"
                    assignedList.add(AssignedItem(classId, className, section))
                    options.add("$className - $section")
                }

                val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                classSpinner.adapter = spinnerAdapter

                classSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        loadStudents(assignedList[position])
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

            } catch (e: Exception) {
                Log.e("FirestoreError", "Error loading assigned classes", e)
            }
        }
    }

    private fun loadStudents(item: AssignedItem) {
        coroutineScope.launch {
            try {
                val studentsSnap = db.collection("school").document(schoolId)
                    .collection("students")
                    .whereEqualTo("class_id", item.classId)
                    .whereEqualTo("section", item.section)
                    .get()
                    .await()

                currentStudents.clear()
                for (stu in studentsSnap) {
                    val stuId = stu.id
                    val name = stu.getString("name") ?: "Unknown"
                    currentStudents.add(StudentItem(stuId, name))
                }

                adapter = StudentAdapter(requireContext(), currentStudents)
                studentListView.adapter = adapter

                if (currentStudents.isEmpty()) {
                    Toast.makeText(requireContext(), "No students found for this class & section", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading students: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AttendanceError", "Error loading students", e)
            }
        }
    }


    private fun saveAttendance() {
        val selectedIndex = classSpinner.selectedItemPosition
        if (selectedIndex < 0 || selectedDate.isEmpty()) {
            Toast.makeText(requireContext(), "Select class and date first", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedClass = assignedList[selectedIndex]
        val attendanceList = currentStudents.map {
            mapOf("stu_id" to it.stuId, "name" to it.name, "status" to if (it.isPresent) "present" else "absent")
        }

        val attendanceData = hashMapOf(
            "attendance_id" to UUID.randomUUID().toString(),
            "class_id" to selectedClass.classId,
            "section" to selectedClass.section,
            "date" to selectedDate,
            "marked_by" to userId,
            "attendance_data" to attendanceList
        )

        db.collection("school").document(schoolId)
            .collection("attendance")
            .add(attendanceData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Attendance submitted successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}

/* Adapter for student list */
class StudentAdapter(context: android.content.Context, private val students: List<AttendanceMarkFragment.StudentItem>)
    : ArrayAdapter<AttendanceMarkFragment.StudentItem>(context, 0, students) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_student_attendance, parent, false)

        val student = students[position]

        val nameText = view.findViewById<TextView>(R.id.studentNameText)
        val checkBox = view.findViewById<CheckBox>(R.id.presentCheckBox)

        val last5Id = if (student.stuId.length >= 5) student.stuId.takeLast(5) else student.stuId
        nameText.text = "${student.name} - $last5Id"

        checkBox.isChecked = student.isPresent
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            student.isPresent = isChecked
        }

        return view
    }
}

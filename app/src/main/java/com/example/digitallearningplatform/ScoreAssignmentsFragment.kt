package com.example.digitallearningplatform

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ScoreAssignmentsFragment : Fragment() {

    private lateinit var classSpinner: Spinner
    private lateinit var assignmentSpinner: Spinner
    private lateinit var listView: ListView
    private lateinit var statusMessage: TextView
    private lateinit var db: FirebaseFirestore

    private lateinit var schoolId: String
    private lateinit var teacherId: String

    private data class AssignedClass(
        val classId: String,
        val className: String,
        val section: String,
        val subject: String
    )

    private data class AssignmentInfo(
        val id: String,
        val name: String,
        val contentUrl: String?,
        val isFile: Boolean
    )

    private data class StudentSubmission(
        val id: String,
        val studentId: String,
        val studentName: String,
        val assignmentId: String,
        val assignmentName: String,
        val assignmentUrl: String,
        val isFile: Boolean,
        val score: Any?,
        val timestamp: Long?,
        val subject: String
    )

    private val assignedClasses = mutableListOf<AssignedClass>()
    private val availableAssignments = mutableListOf<AssignmentInfo>()
    private val studentSubmissions = mutableListOf<StudentSubmission>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_score_assignments, container, false)

        classSpinner = view.findViewById(R.id.classSpinner)
        assignmentSpinner = view.findViewById(R.id.assignmentSpinner)
        listView = view.findViewById(R.id.studentList2)
        statusMessage = view.findViewById(R.id.statusMessage)

        db = FirebaseFirestore.getInstance()
        schoolId = arguments?.getString("school_id") ?: ""
        teacherId = arguments?.getString("user_id") ?: ""

        if (schoolId.isEmpty() || teacherId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing teacher or school info", Toast.LENGTH_SHORT).show()
            return view
        }

        loadAssignedClasses()

        classSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (assignedClasses.isNotEmpty()) {
                    val selectedClass = assignedClasses[position]
                    loadAssignments(selectedClass)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        assignmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (availableAssignments.isNotEmpty()) {
                    val selectedClass = assignedClasses[classSpinner.selectedItemPosition]
                    val selectedAssignment = availableAssignments[position]
                    loadSubmissions(selectedClass, selectedAssignment.id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return view
    }

    /** Step 1: Load classes assigned to the teacher **/
    private fun loadAssignedClasses() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val query = db.collection("school").document(schoolId)
                    .collection("classAssignments")
                    .whereEqualTo("staff_id", teacherId)
                    .get().await()

                assignedClasses.clear()
                val classLabels = mutableListOf<String>()

                for (doc in query) {
                    val classId = doc.getString("class_id") ?: continue
                    val section = doc.getString("section") ?: "N/A"
                    val subject = doc.getString("subject") ?: "N/A"

                    val classDoc = db.collection("school").document(schoolId)
                        .collection("classes").document(classId)
                        .get().await()
                    val className = classDoc.getString("class_name") ?: "Unknown Class"

                    assignedClasses.add(AssignedClass(classId, className, section, subject))
                    classLabels.add("$className - $section - $subject")
                }

                if (classLabels.isNotEmpty()) {
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classLabels)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    classSpinner.adapter = adapter
                } else {
                    statusMessage.text = "No classes assigned."
                }

            } catch (e: Exception) {
                Log.e("FirestoreError", "Error loading classes", e)
                statusMessage.text = "Error fetching classes: ${e.message}"
            }
        }
    }

    /** Step 2: Load assignments uploaded by teacher for that class **/
    private fun loadAssignments(selectedClass: AssignedClass) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val query = db.collection("school").document(schoolId)
                    .collection("content")
                    .whereEqualTo("class_id", selectedClass.classId)
                    .whereEqualTo("section", selectedClass.section)
                    .whereEqualTo("subject_name", selectedClass.subject)
                    .whereEqualTo("uploaded_by", teacherId)
                    .whereEqualTo("type", "Assignment")
                    .get().await()

                availableAssignments.clear()

                for (doc in query) {
                    availableAssignments.add(
                        AssignmentInfo(
                            id = doc.id,
                            name = doc.getString("assignment_name") ?: "Untitled Assignment",
                            contentUrl = doc.getString("content_url"),
                            isFile = doc.getBoolean("is_file") ?: false
                        )
                    )
                }

                if (availableAssignments.isNotEmpty()) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        availableAssignments.map { it.name }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    assignmentSpinner.adapter = adapter
                } else {
                    statusMessage.text = "No assignments uploaded for this class."
                    listView.adapter = null
                }

            } catch (e: Exception) {
                Log.e("FirestoreError", "Error loading assignments", e)
                statusMessage.text = "Error fetching assignments: ${e.message}"
            }
        }
    }

    /** Step 3: Load student submissions (filter by assignment_id) **/
    private fun loadSubmissions(selectedClass: AssignedClass, assignmentId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val query = db.collection("school").document(schoolId)
                    .collection("progress")
                    .whereEqualTo("class_id", selectedClass.classId)
                    .whereEqualTo("section", selectedClass.section)
                    .whereEqualTo("subject_name", selectedClass.subject)
                    .whereEqualTo("assignment_id", assignmentId)
                    .get().await()

                studentSubmissions.clear()
                for (doc in query) {
                    val sub = StudentSubmission(
                        id = doc.id,
                        studentId = doc.getString("student_id") ?: "",
                        studentName = doc.getString("student_name") ?: "Unknown",
                        assignmentId = doc.getString("assignment_id") ?: "",
                        assignmentName = doc.getString("assignment_name") ?: "N/A",
                        assignmentUrl = doc.getString("assignment_url") ?: "",
                        isFile = doc.getBoolean("is_file") ?: false,
                        score = doc.get("score"),
                        timestamp = doc.getLong("timestamp"),
                        subject = doc.getString("subject_name") ?: "N/A"
                    )
                    studentSubmissions.add(sub)
                }

                if (studentSubmissions.isEmpty()) {
                    statusMessage.text = "No student submissions yet."
                    listView.adapter = null
                } else {
                    setupListView()
                }

            } catch (e: Exception) {
                Log.e("FirestoreError", "Error loading submissions", e)
                statusMessage.text = "Error fetching submissions: ${e.message}"
            }
        }
    }

    /** Step 4: Display submissions and scores **/
    private fun setupListView() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            studentSubmissions.map {
                val dateStr = it.timestamp?.let { ts ->
                    java.text.SimpleDateFormat("dd MMM yyyy").format(java.util.Date(ts))
                } ?: "Unknown date"
                "${it.studentName} — Score: ${it.score ?: "Not graded yet"} — ($dateStr)"
            }
        )
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val submission = studentSubmissions[position]
            showSubmissionDialog(submission)
        }
    }

    /** Step 5: Open submission or grade **/
    private fun showSubmissionDialog(sub: StudentSubmission) {
        val options = mutableListOf<String>()
        options.add(if (sub.isFile) "Download File" else "Open Link")
        options.add("Add / Update Score")

        AlertDialog.Builder(requireContext())
            .setTitle("${sub.studentName}'s Submission")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Download File" -> openInBrowser(sub.assignmentUrl)
                    "Open Link" -> openInBrowser(sub.assignmentUrl)
                    "Add / Update Score" -> showScoreInputDialog(sub)
                }
            }
            .show()
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Step 6: Add or update score **/
    private fun showScoreInputDialog(submission: StudentSubmission) {
        val input = EditText(requireContext())
        input.hint = "Enter score (e.g., 20/30)"

        AlertDialog.Builder(requireContext())
            .setTitle("Grade ${submission.studentName}")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                val scoreText = input.text.toString().trim()
                if (scoreText.matches(Regex("""\d{1,2}/\d{1,2}"""))) {
                    updateScore(submission, scoreText)
                } else {
                    Toast.makeText(requireContext(), "Enter valid format (e.g. 20/30)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateScore(submission: StudentSubmission, score: String) {
        db.collection("school").document(schoolId)
            .collection("progress").document(submission.id)
            .update(mapOf("score" to score, "graded_by" to teacherId))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Score updated successfully", Toast.LENGTH_SHORT).show()
                val selectedClass = assignedClasses[classSpinner.selectedItemPosition]
                val selectedAssignment = availableAssignments[assignmentSpinner.selectedItemPosition]
                loadSubmissions(selectedClass, selectedAssignment.id)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error updating score: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

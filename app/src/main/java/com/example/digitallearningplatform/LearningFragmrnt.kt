package com.example.digitallearningplatform

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class LearningFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var statusMessage: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var studentId: String
    private lateinit var studentName: String
    private lateinit var classId: String
    private lateinit var className: String
    private lateinit var section: String
    private lateinit var schoolId: String

    private val PICK_FILE_REQUEST = 3001
    private var selectedAssignmentId: String? = null
    private var selectedFileUri: Uri? = null

    data class ContentItem(
        val id: String,
        val title: String,
        val subject: String,
        val instructions: String,
        val contentUrl: String,
        val isFile: Boolean,
        val type: String,
        val deadline: Long?,
        var score: String? = null // <-- Added field for score display
    )

    private val contentList = mutableListOf<ContentItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_learning, container, false)
        listView = view.findViewById(R.id.assignmentList2)
        statusMessage = view.findViewById(R.id.statusMessage)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        schoolId = arguments?.getString("school_id") ?: ""
        studentId = arguments?.getString("user_id") ?: ""
        classId = arguments?.getString("class_id") ?: ""
        section = arguments?.getString("section") ?: ""

        lifecycleScope.launch {
            fetchStudentAndClassDetails()
            loadContent()
        }

        return view
    }

    /** Fetch student and class details **/
    private suspend fun fetchStudentAndClassDetails() {
        try {
            val studentDoc = db.collection("school").document(schoolId)
                .collection("students").document(studentId).get().await()
            studentName = studentDoc.getString("name") ?: "Unknown Student"

            val classDoc = db.collection("school").document(schoolId)
                .collection("classes").document(classId).get().await()
            className = classDoc.getString("class_name") ?: "Unknown Class"
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error fetching details", e)
            studentName = "Unknown"
            className = "Unknown"
        }
    }

    /** Load content for student’s class **/
    private fun loadContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val query = db.collection("school").document(schoolId)
                    .collection("content")
                    .whereEqualTo("class_id", classId)
                    .whereEqualTo("section", section)
                    .get().await()

                contentList.clear()
                for (doc in query.documents) {
                    val item = ContentItem(
                        id = doc.id,
                        title = doc.getString("assignment_name")
                            ?: doc.getString("content_name")
                            ?: "Untitled",
                        subject = doc.getString("subject_name") ?: "Unknown Subject",
                        instructions = doc.getString("instructions") ?: "",
                        contentUrl = doc.getString("content_url") ?: "",
                        isFile = doc.getBoolean("is_file") ?: false,
                        type = doc.getString("type") ?: "Material",
                        deadline = doc.getLong("deadline"),
                        score = null
                    )
                    contentList.add(item)
                }

                if (contentList.isEmpty()) {
                    statusMessage.text = "No content found for this class."
                } else {
                    fetchScoresAndSetupList()
                }

            } catch (e: Exception) {
                Log.e("FirestoreError", "Error loading content", e)
                statusMessage.text = "Error loading data: ${e.message}"
            }
        }
    }

    /** Fetch scores from progress table **/
    private suspend fun fetchScoresAndSetupList() {
        try {
            val progressDocs = db.collection("school").document(schoolId)
                .collection("progress")
                .whereEqualTo("student_id", studentId)
                .get()
                .await()

            for (progress in progressDocs.documents) {
                val assignmentId = progress.getString("assignment_id")
                val score = progress.get("score")?.toString()
                assignmentId?.let {
                    contentList.find { it.id == assignmentId }?.score = score
                }
            }

            setupListView()
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error fetching progress", e)
            setupListView()
        }
    }

    /** Show content list with “marks or not scored yet” **/
    private fun setupListView() {
        val adapter = object : ArrayAdapter<ContentItem>(
            requireContext(),
            R.layout.item_content_row,
            contentList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val inflater = LayoutInflater.from(context)
                val view = convertView ?: inflater.inflate(R.layout.item_content_row, parent, false)

                val title = view.findViewById<TextView>(R.id.contentTitle)
                val subject = view.findViewById<TextView>(R.id.contentSubject)
                val scoreText = view.findViewById<TextView>(R.id.scoreText)
                val viewBtn = view.findViewById<Button>(R.id.viewButton)
                val submitBtn = view.findViewById<Button>(R.id.submitButton)

                val item = getItem(position)!!

                title.text = item.title
                subject.text = "${item.subject} (${item.type})"

                if (item.score != null) {
                    scoreText.text = "Score: ${item.score}"
                } else {
                    scoreText.text = "Not Scored Yet"
                }

                viewBtn.setOnClickListener { openInBrowser(item.contentUrl) }

                if (item.type.equals("Assignment", ignoreCase = true)) {
                    submitBtn.visibility = View.VISIBLE
                    submitBtn.setOnClickListener {
                        selectedAssignmentId = item.id
                        showSubmissionDialog(item)
                    }
                } else {
                    submitBtn.visibility = View.GONE
                }

                return view
            }
        }
        listView.adapter = adapter
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSubmissionDialog(item: ContentItem) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Submit Assignment - ${item.title}")
            .setMessage("Choose how you want to submit your work:")
            .setPositiveButton("Upload File") { _, _ -> selectFile() }
            .setNegativeButton("Paste Link") { _, _ -> showLinkInputDialog(item) }
            .setNeutralButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, PICK_FILE_REQUEST)
    }

    private fun showLinkInputDialog(item: ContentItem) {
        val input = EditText(requireContext())
        input.hint = "Paste your assignment link"
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Submit Link")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                val link = input.text.toString().trim()
                if (link.isNotEmpty()) uploadProgress(item, link, false)
                else Toast.makeText(requireContext(), "Enter valid link", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadProgress(item: ContentItem, url: String, isFile: Boolean) {
        val progressData = hashMapOf(
            "progress_id" to UUID.randomUUID().toString(),
            "student_id" to studentId,
            "student_name" to studentName,
            "class_id" to classId,
            "class_name" to className,
            "section" to section,
            "subject_name" to item.subject,
            "assignment_id" to item.id,
            "assignment_name" to item.title,
            "assignment_url" to url,
            "is_file" to isFile,
            "timestamp" to System.currentTimeMillis(),
            "score" to null,
            "graded_by" to null
        )

        db.collection("school").document(schoolId)
            .collection("progress")
            .add(progressData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Assignment submitted successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Submission failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadFileToStorage(fileUri: Uri) {
        val ref = storage.reference.child("student_submissions/${System.currentTimeMillis()}_${fileUri.lastPathSegment}")
        ref.putFile(fileUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val item = contentList.find { it.id == selectedAssignmentId } ?: return@addOnSuccessListener
                    uploadProgress(item, uri.toString(), true)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "File upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let { uploadFileToStorage(it) }
        }
    }
}

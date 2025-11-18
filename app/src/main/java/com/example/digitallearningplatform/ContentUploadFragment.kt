package com.example.digitallearningplatform

import android.app.Activity
import android.app.DatePickerDialog
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ContentUploadFragment : Fragment() {

    private lateinit var assignmentSpinner: Spinner
    private lateinit var typeSpinner: Spinner
    private lateinit var radioFile: RadioButton
    private lateinit var radioLink: RadioButton
    private lateinit var linkInput: EditText
    private lateinit var instructionInput: EditText
    private lateinit var selectFileButton: Button
    private lateinit var uploadButton: Button
    private lateinit var statusMessage: TextView
    private lateinit var deadlineButton: Button
    private lateinit var deadlineText: TextView

    // ✅ Added field for assignment name
    private lateinit var assignmentNameInput: EditText

    private lateinit var schoolId: String
    private lateinit var userId: String

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var selectedFileUri: Uri? = null
    private val PICK_FILE_REQUEST = 2001
    private var selectedDeadline: Long? = null

    private data class AssignedItem(
        val classId: String,
        val className: String,
        val section: String,
        val subject: String
    )

    private val assignedList = mutableListOf<AssignedItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_content_upload, container, false)

        // Initialize views
        assignmentSpinner = view.findViewById(R.id.assignmentSpinner)
        typeSpinner = view.findViewById(R.id.typeSpinner)
        radioFile = view.findViewById(R.id.radioFile)
        radioLink = view.findViewById(R.id.radioLink)
        linkInput = view.findViewById(R.id.linkInput)
        instructionInput = view.findViewById(R.id.instructionInput)
        selectFileButton = view.findViewById(R.id.selectFileButton)
        uploadButton = view.findViewById(R.id.uploadButton)
        statusMessage = view.findViewById(R.id.statusMessage)
        deadlineButton = view.findViewById(R.id.deadlineButton)
        deadlineText = view.findViewById(R.id.deadlineText)

        // ✅ Initialize assignment name field
        assignmentNameInput = view.findViewById(R.id.assignmentNameInput)

        schoolId = arguments?.getString("school_id") ?: ""
        userId = arguments?.getString("user_id") ?: ""

        if (schoolId.isEmpty() || userId.isEmpty()) {
            context?.let {
                Toast.makeText(it, "Missing user or school info", Toast.LENGTH_SHORT).show()
            }
            return view
        }

        setupTypeSpinner()
        setupRadioBehavior()
        loadAssignedClasses()

        selectFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, PICK_FILE_REQUEST)
        }

        deadlineButton.setOnClickListener { showDatePicker() }
        uploadButton.setOnClickListener { uploadSelectedContent() }

        return view
    }

    private fun setupTypeSpinner() {
        val types = listOf("Learning Material", "Assignment")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter
    }

    private fun setupRadioBehavior() {
        radioFile.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                linkInput.visibility = View.GONE
                selectFileButton.visibility = View.VISIBLE
            }
        }
        radioLink.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                linkInput.visibility = View.VISIBLE
                selectFileButton.visibility = View.GONE
            }
        }
    }

    private fun loadAssignedClasses() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val assignmentSnap = db.collection("school").document(schoolId)
                    .collection("classAssignments")
                    .whereEqualTo("staff_id", userId)
                    .get()
                    .await()

                if (assignmentSnap.isEmpty) {
                    context?.let {
                        Toast.makeText(it, "No assigned classes found", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                assignedList.clear()
                val assignmentLabels = mutableListOf<String>()

                for (doc in assignmentSnap) {
                    val classId = doc.getString("class_id") ?: continue
                    val section = doc.getString("section") ?: "N/A"
                    val subject = doc.getString("subject") ?: "N/A"

                    val classDoc = db.collection("school").document(schoolId)
                        .collection("classes").document(classId)
                        .get().await()

                    val className = classDoc.getString("class_name") ?: "Unknown Class"

                    val item = AssignedItem(classId, className, section, subject)
                    assignedList.add(item)
                    assignmentLabels.add("${item.className} - ${item.section} - ${item.subject}")
                }

                context?.let {
                    val adapter = ArrayAdapter(it, android.R.layout.simple_spinner_item, assignmentLabels)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    assignmentSpinner.adapter = adapter
                }

            } catch (e: Exception) {
                Log.e("FirestoreError", "Error loading assigned classes", e)
                context?.let {
                    Toast.makeText(it, "Error fetching assignments: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day, 23, 59, 59)
                selectedDeadline = calendar.timeInMillis
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                deadlineText.text = "Deadline: ${sdf.format(calendar.time)}"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun uploadSelectedContent() {
        val selectedItem = assignedList.getOrNull(assignmentSpinner.selectedItemPosition)
        if (selectedItem == null) {
            Toast.makeText(requireContext(), "Please select a class assignment", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedType = typeSpinner.selectedItem.toString()
        val instructions = instructionInput.text.toString().trim()
        val assignmentName = assignmentNameInput.text.toString().trim() // ✅ Added

        if (selectedType == "Assignment" && assignmentName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter assignment name", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedType == "Assignment" && selectedDeadline == null) {
            Toast.makeText(requireContext(), "Please set a deadline", Toast.LENGTH_SHORT).show()
            return
        }

        if (radioLink.isChecked) {
            val link = linkInput.text.toString().trim()
            if (link.isEmpty()) {
                Toast.makeText(requireContext(), "Please paste a valid link", Toast.LENGTH_SHORT).show()
                return
            }
            uploadContentToFirestore(selectedItem, link, isFile = false, selectedType, instructions, assignmentName)
        } else {
            selectedFileUri?.let {
                uploadFileToStorage(selectedItem, it, selectedType, instructions, assignmentName)
            } ?: Toast.makeText(requireContext(), "Please select a file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadFileToStorage(
        item: AssignedItem,
        fileUri: Uri,
        type: String,
        instructions: String,
        assignmentName: String
    ) {
        val fileRef = storage.reference.child("uploads/${System.currentTimeMillis()}_${fileUri.lastPathSegment}")
        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    uploadContentToFirestore(item, uri.toString(), isFile = true, type, instructions, assignmentName)
                }
            }
            .addOnFailureListener {
                statusMessage.text = "File upload failed: ${it.message}"
            }
    }

    private fun uploadContentToFirestore(
        item: AssignedItem,
        contentUrl: String,
        isFile: Boolean,
        type: String,
        instructions: String,
        assignmentName: String
    ) {
        val data = hashMapOf(
            "class_id" to item.classId,
            "class_name" to item.className,
            "section" to item.section,
            "subject_name" to item.subject,
            "content_url" to contentUrl,
            "is_file" to isFile,
            "type" to type,
            "uploaded_by" to userId,
            "instructions" to instructions,
            "deadline" to selectedDeadline,
            "timestamp" to System.currentTimeMillis(),
            "assignment_name" to assignmentName // ✅ Added
        )

        db.collection("school").document(schoolId)
            .collection("content")
            .add(data)
            .addOnSuccessListener {
                statusMessage.text = "Content uploaded successfully!"
            }
            .addOnFailureListener {
                statusMessage.text = "Failed to upload: ${it.message}"
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            statusMessage.text = "File selected: ${selectedFileUri?.lastPathSegment}"
        }
    }
}

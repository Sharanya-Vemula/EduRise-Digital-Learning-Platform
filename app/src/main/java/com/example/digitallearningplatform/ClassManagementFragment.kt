package com.example.digitallearningplatform

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

data class ClassItem(
    val className: String,
    val section: String,
    val subject: String
)

class ClassManagementFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClassAdapter
    private val classList = mutableListOf<ClassItem>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var schoolId: String
    private lateinit var staffId: String
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var sendButton: Button
    private lateinit var titleEditText: EditText
    private lateinit var messageEditText: EditText

    // Store selected class|section pairs
    private val selectedClassesForNotification = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_class_management, container, false)

        recyclerView = view.findViewById(R.id.classRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        sendButton = view.findViewById(R.id.sendNotificationButton)
        titleEditText = view.findViewById(R.id.notificationTitleEdit)
        messageEditText = view.findViewById(R.id.notificationMessageEdit)

        adapter = ClassAdapter(classList,
            onItemClick = { item ->
                Toast.makeText(requireContext(), "Clicked: ${item.className}", Toast.LENGTH_SHORT).show()
            },
            onNotificationCheck = { item, isChecked ->
                // ✅ Combine class and section uniquely
                val key = "${item.className}|${item.section}"
                if (isChecked) selectedClassesForNotification.add(key)
                else selectedClassesForNotification.remove(key)
            }
        )

        recyclerView.adapter = adapter

        // Get arguments passed to fragment
        schoolId = arguments?.getString("school_id").orEmpty()
        staffId = arguments?.getString("user_id").orEmpty()

        if (schoolId.isEmpty() || staffId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing school or staff ID!", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("ClassDebug", "Using schoolId=$schoolId, staffId=$staffId")
            loadAssignedClasses(staffId)
        }

        sendButton.setOnClickListener { sendNotification() }

        return view
    }

    /** Load teacher's assigned classes from Firestore */
    private fun loadAssignedClasses(staffId: String) {
        coroutineScope.launch {
            try {
                val assignmentSnap = db.collection("school").document(schoolId)
                    .collection("classAssignments")
                    .whereEqualTo("staff_id", staffId)
                    .get()
                    .await()

                classList.clear()

                if (assignmentSnap.isEmpty) {
                    Toast.makeText(requireContext(), "No classes assigned yet", Toast.LENGTH_SHORT).show()
                    adapter.notifyDataSetChanged()
                    return@launch
                }

                val assignments = assignmentSnap.documents.mapNotNull { doc ->
                    val classId = doc.getString("class_id")
                    val subject = doc.getString("subject") ?: "N/A"
                    val section = doc.getString("section") ?: "N/A"
                    if (classId != null) Triple(classId, section, subject) else null
                }

                // Fetch class details concurrently
                val classDocs = assignments.map { (classId, section, subject) ->
                    async(Dispatchers.IO) {
                        val classDoc = db.collection("school").document(schoolId)
                            .collection("classes").document(classId)
                            .get().await()

                        if (classDoc.exists()) {
                            val className = classDoc.getString("class_name") ?: "Unknown Class"
                            ClassItem(className, section, subject)
                        } else null
                    }
                }.awaitAll().filterNotNull()

                classList.addAll(classDocs)
                adapter.notifyDataSetChanged()

            } catch (e: Exception) {
                Log.e("FirestoreError", "Error loading assigned classes", e)
                Toast.makeText(requireContext(), "Error fetching assignments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Send notification to all selected class-section pairs */
    private fun sendNotification() {
        val title = titleEditText.text.toString().trim()
        val message = messageEditText.text.toString().trim()

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(requireContext(), "Enter title and message", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedClassesForNotification.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one class", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDateTime =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // ✅ Store classes as list of maps {class, section}
        val selectedClassSectionList = selectedClassesForNotification.map { key ->
            val (className, section) = key.split("|")
            mapOf("class" to className, "section" to section)
        }

        val notificationData = hashMapOf(
            "title" to title,
            "message" to message,
            "classes" to selectedClassSectionList,
            "marked_by" to staffId,
            "created_at" to currentDateTime
        )

        db.collection("school").document(schoolId)
            .collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Notification sent!", Toast.LENGTH_SHORT).show()
                titleEditText.text.clear()
                messageEditText.text.clear()
                selectedClassesForNotification.clear()
                adapter.clearSelections()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }

    /** Adapter class inside Fragment.kt */
    class ClassAdapter(
        private val classes: List<ClassItem>,
        private val onItemClick: (ClassItem) -> Unit,
        private val onNotificationCheck: (ClassItem, Boolean) -> Unit
    ) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

        private val selectedMap = mutableMapOf<Int, Boolean>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_class, parent, false)
            return ClassViewHolder(view)
        }

        override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
            val item = classes[position]
            holder.classText.text = "${item.className} - ${item.section} (${item.subject})"

            holder.itemView.setOnClickListener { onItemClick(item) }

            holder.notifyCheckBox.setOnCheckedChangeListener(null)
            holder.notifyCheckBox.isChecked = selectedMap[position] ?: false
            holder.notifyCheckBox.setOnCheckedChangeListener { _, isChecked ->
                selectedMap[position] = isChecked
                onNotificationCheck(item, isChecked)
            }
        }

        override fun getItemCount(): Int = classes.size

        fun clearSelections() {
            selectedMap.clear()
            notifyDataSetChanged()
        }

        class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val classText: TextView = itemView.findViewById(R.id.classText)
            val notifyCheckBox: CheckBox = itemView.findViewById(R.id.notifyCheckBox)
        }
    }
}

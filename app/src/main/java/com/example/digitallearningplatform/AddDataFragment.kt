package com.example.digitallearningplatform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.util.*

class AddDataFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var schoolId: String

    private lateinit var etClassName: EditText
    private lateinit var etSubjectName: EditText
    private lateinit var etSectionName: EditText
    private lateinit var btnAddClass: Button
    private lateinit var btnAddSubject: Button
    private lateinit var btnAddSection: Button
    private lateinit var lvClasses: ListView
    private lateinit var lvSubjects: ListView
    private lateinit var lvSections: ListView
    private lateinit var spinnerClassSelect: Spinner

    private val classList = mutableListOf<String>()
    private val subjectList = mutableListOf<String>()
    private val sectionList = mutableListOf<String>()
    private val classIds = mutableListOf<String>()

    private lateinit var classAdapter: ArrayAdapter<String>
    private lateinit var subjectAdapter: ArrayAdapter<String>
    private lateinit var sectionAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_data, container, false)

        firestore = FirebaseFirestore.getInstance()
        schoolId = (activity as? AdminDashboard)?.intent?.getStringExtra("school_id") ?: ""

        // UI elements
        etClassName = view.findViewById(R.id.etClassName)
        etSubjectName = view.findViewById(R.id.etSubjectName)
        etSectionName = view.findViewById(R.id.etSectionName)
        btnAddClass = view.findViewById(R.id.btnAddClass)
        btnAddSubject = view.findViewById(R.id.btnAddSubject)
        btnAddSection = view.findViewById(R.id.btnAddSection)
        lvClasses = view.findViewById(R.id.lvClasses)
        lvSubjects = view.findViewById(R.id.lvSubjects)
        lvSections = view.findViewById(R.id.lvSections)
        spinnerClassSelect = view.findViewById(R.id.spinnerSelectClass)

        // Adapters
        classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, classList)
        subjectAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, subjectList)
        sectionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sectionList)

        lvClasses.adapter = classAdapter
        lvSubjects.adapter = subjectAdapter
        lvSections.adapter = sectionAdapter

        // Button actions
        btnAddClass.setOnClickListener { addClass() }
        btnAddSubject.setOnClickListener { addSubject() }
        btnAddSection.setOnClickListener { addSection() }

        // Load existing data
        loadClasses()
        loadSubjects()

        // On selecting a class, load its sections
        spinnerClassSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedClassId = classIds[position]
                loadSections(selectedClassId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return view
    }

    private fun addClass() {
        val classText = etClassName.text.toString().trim()
        if (classText.isEmpty()) {
            Toast.makeText(requireContext(), "Enter class name", Toast.LENGTH_SHORT).show()
            return
        }

        val classId = UUID.randomUUID().toString()
        val classData = hashMapOf(
            "class_id" to classId,
            "class_name" to classText,
            "section_count" to 0
        )

        firestore.collection("school").document(schoolId)
            .collection("classes")
            .document(classId)
            .set(classData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Class added!", Toast.LENGTH_SHORT).show()
                etClassName.text.clear()
                loadClasses()
            }
    }

    private fun addSubject() {
        val subjectText = etSubjectName.text.toString().trim()
        if (subjectText.isEmpty()) {
            Toast.makeText(requireContext(), "Enter subject name", Toast.LENGTH_SHORT).show()
            return
        }

        val subjectId = UUID.randomUUID().toString()
        val subjectData = hashMapOf(
            "subject_id" to subjectId,
            "subject_name" to subjectText
        )

        firestore.collection("school").document(schoolId)
            .collection("subjects")
            .document(subjectId)
            .set(subjectData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Subject added!", Toast.LENGTH_SHORT).show()
                etSubjectName.text.clear()
                loadSubjects()
            }
    }

    private fun addSection() {
        val sectionText = etSectionName.text.toString().trim()
        val selectedClassPos = spinnerClassSelect.selectedItemPosition

        if (sectionText.isEmpty() || selectedClassPos == -1) {
            Toast.makeText(requireContext(), "Enter section name and select class", Toast.LENGTH_SHORT).show()
            return
        }

        val classId = classIds[selectedClassPos]
        val classRef = firestore.collection("school").document(schoolId)
            .collection("classes").document(classId)

        // Use Firestore arrayUnion to add section in list
        classRef.update("sections", com.google.firebase.firestore.FieldValue.arrayUnion(sectionText))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Section added successfully!", Toast.LENGTH_SHORT).show()
                etSectionName.text.clear()
                loadSections(classId) // reload updated list
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadClasses() {
        firestore.collection("school").document(schoolId)
            .collection("classes")
            .get()
            .addOnSuccessListener { snapshot ->
                classList.clear()
                classIds.clear()
                for (doc: QueryDocumentSnapshot in snapshot) {
                    classList.add(doc.getString("class_name") ?: "")
                    classIds.add(doc.id)
                }
                classAdapter.notifyDataSetChanged()

                val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classList)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerClassSelect.adapter = spinnerAdapter
            }
    }

    private fun loadSubjects() {
        firestore.collection("school").document(schoolId)
            .collection("subjects")
            .get()
            .addOnSuccessListener { snapshot ->
                subjectList.clear()
                for (doc: QueryDocumentSnapshot in snapshot) {
                    subjectList.add(doc.getString("subject_name") ?: "")
                }
                subjectAdapter.notifyDataSetChanged()
            }
    }

    private fun loadSections(classId: String) {
        firestore.collection("school").document(schoolId)
            .collection("classes").document(classId)
            .collection("sections")
            .get()
            .addOnSuccessListener { snapshot ->
                sectionList.clear()
                for (doc: QueryDocumentSnapshot in snapshot) {
                    sectionList.add(doc.getString("section_name") ?: "")
                }
                sectionAdapter.notifyDataSetChanged()
            }
    }
}

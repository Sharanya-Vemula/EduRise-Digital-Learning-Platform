package com.example.digitallearningplatform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ClassAssignmentFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var schoolId: String

    private lateinit var teacherSpinner: Spinner
    private lateinit var classSpinner: Spinner
    private lateinit var sectionSpinner: Spinner
    private lateinit var subjectSpinner: Spinner
    private lateinit var isInchargeCheckbox: CheckBox
    private lateinit var assignBtn: Button

    private val teacherList = mutableListOf<Pair<String, String>>() // (id, name)
    private val classList = mutableListOf<Pair<String, String>>()   // (id, name)
    private val sectionList = mutableListOf<String>()               // section names only
    private val subjectList = mutableListOf<Pair<String, String>>() // (id, name)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_class_assignment, container, false)

        firestore = FirebaseFirestore.getInstance()
        schoolId = (activity as? AdminDashboard)?.intent?.getStringExtra("school_id") ?: ""

        teacherSpinner = view.findViewById(R.id.spinnerTeacher)
        classSpinner = view.findViewById(R.id.spinnerClass)
        sectionSpinner = view.findViewById(R.id.spinnerSection)
        subjectSpinner = view.findViewById(R.id.spinnerSubject)
        isInchargeCheckbox = view.findViewById(R.id.checkboxIncharge)
        assignBtn = view.findViewById(R.id.btnAssign)

        loadTeachers()
        loadClasses()
        loadSubjects()

        // When class is selected → load sections for that class
        classSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val classId = classList[position].first
                loadSections(classId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        assignBtn.setOnClickListener { assignClass() }

        return view
    }

    private fun loadTeachers() {
        firestore.collection("school").document(schoolId)
            .collection("staff")
            .get()
            .addOnSuccessListener { snapshot ->
                teacherList.clear()
                for (doc in snapshot) {
                    val id = doc.id
                    val name = doc.getString("name") ?: id
                    teacherList.add(Pair(id, name))
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teacherList.map { it.second })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                teacherSpinner.adapter = adapter
            }
    }

    private fun loadClasses() {
        firestore.collection("school").document(schoolId)
            .collection("classes")
            .get()
            .addOnSuccessListener { snapshot ->
                classList.clear()
                for (doc in snapshot) {
                    val id = doc.id
                    val name = doc.getString("class_name") ?: id
                    classList.add(Pair(id, name))
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classList.map { it.second })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                classSpinner.adapter = adapter
            }
    }

    private fun loadSections(classId: String) {
        firestore.collection("school").document(schoolId)
            .collection("classes").document(classId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    sectionList.clear()
                    val sections = document.get("sections") as? List<String> ?: emptyList()
                    sectionList.addAll(sections)

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sectionList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    sectionSpinner.adapter = adapter
                } else {
                    Toast.makeText(requireContext(), "Class not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load sections: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadSubjects() {
        firestore.collection("school").document(schoolId)
            .collection("subjects")
            .get()
            .addOnSuccessListener { snapshot ->
                subjectList.clear()
                for (doc in snapshot) {
                    val id = doc.id
                    val name = doc.getString("subject_name") ?: id
                    subjectList.add(Pair(id, name))
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subjectList.map { it.second })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                subjectSpinner.adapter = adapter
            }
    }

    private fun assignClass() {
        val teacherPos = teacherSpinner.selectedItemPosition
        val classPos = classSpinner.selectedItemPosition
        val subjectPos = subjectSpinner.selectedItemPosition
        val sectionPos = sectionSpinner.selectedItemPosition

        if (teacherPos == -1 || classPos == -1 || subjectPos == -1 || sectionPos == -1) {
            Toast.makeText(requireContext(), "Select all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val teacherId = teacherList[teacherPos].first
        val classId = classList[classPos].first
        val subjectName = subjectList[subjectPos].second
        val sectionName = sectionList[sectionPos]
        val isIncharge = isInchargeCheckbox.isChecked

        val assignmentData = hashMapOf(
            "staff_id" to teacherId,
            "class_id" to classId,
            "subject" to subjectName,
            "section" to sectionName,
            "is_incharge" to isIncharge,
            "assigned_at" to Date()
        )

        firestore.collection("school").document(schoolId)
            .collection("classAssignments")
            .add(assignmentData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Class assigned successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error assigning class", Toast.LENGTH_SHORT).show()
            }
    }
}

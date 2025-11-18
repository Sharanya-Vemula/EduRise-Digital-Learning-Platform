package com.example.digitallearningplatform


import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

class SyncManager(private val context: Context) {

    private val dbHelper = EduRiseDatabaseHelper(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun syncSchoolData(schoolCode: String) {
        ioScope.launch {
            try {
                val db = dbHelper.writableDatabase

                // 1️⃣ Fetch school document
                val schoolRef = firestore.collection("schools").document(schoolCode)
                val schoolSnap = schoolRef.get().await()

                if (schoolSnap.exists()) {
                    val schoolId = schoolSnap.getString("school_id") ?: UUID.randomUUID().toString()
                    Log.d("Sync", "Syncing data for school: $schoolCode")

                    // 2️⃣ Sync staff
                    syncCollectionToSQLite(db, schoolRef.collection("staff"), "staff", "staff_id", schoolId)

                    // 3️⃣ Sync students
                    syncCollectionToSQLite(db, schoolRef.collection("students"), "students", "student_id", schoolId)

                    // 4️⃣ Sync classes
                    syncCollectionToSQLite(db, schoolRef.collection("classes"), "classes", "class_id", schoolId)

                    // 5️⃣ Sync subjects
                    syncCollectionToSQLite(db, schoolRef.collection("subjects"), "subjects", "subject_id", schoolId)

                    // 6️⃣ Sync content
                    syncCollectionToSQLite(db, schoolRef.collection("content"), "content", "content_id", schoolId)

                    // 7️⃣ Sync attendance
                    syncCollectionToSQLite(db, schoolRef.collection("attendance"), "attendance", "attendance_id", schoolId)

                    // 8️⃣ Sync analytics
                    syncCollectionToSQLite(db, schoolRef.collection("analytics"), "analytics", "analytics_id", schoolId)
                }

            } catch (e: Exception) {
                Log.e("SyncManager", "Sync failed: ${e.message}")
            }
        }
    }

    private suspend fun syncCollectionToSQLite(
        db: SQLiteDatabase,
        collectionRef: com.google.firebase.firestore.CollectionReference,
        tableName: String,
        idColumn: String,
        schoolId: String
    ) {
        val snapshots = collectionRef.get().await()
        db.beginTransaction()
        try {
            for (doc in snapshots.documents) {
                val data = doc.data ?: continue
                val values = ContentValues()

                data.forEach { (key, value) ->
                    if (value != null) values.put(key, value.toString())
                }
                values.put("school_id", schoolId)

                // Replace or insert (UPSERT)
                db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            Log.d("SyncManager", "✅ Synced $tableName (${snapshots.size()} rows)")
        } catch (e: Exception) {
            Log.e("SyncManager", "Error syncing $tableName: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }
}

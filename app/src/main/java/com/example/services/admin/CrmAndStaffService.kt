package com.example.services.admin

import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CrmAndStaffService(private val firestore: FirebaseFirestore) {

    private val _staffMembers = MutableStateFlow<List<StaffMember>>(getInitialStaff())
    val staffMembers: StateFlow<List<StaffMember>> = _staffMembers.asStateFlow()

    private val _cannedResponses = MutableStateFlow<List<CannedResponseTemplate>>(getInitialCannedResponses())
    val cannedResponses: StateFlow<List<CannedResponseTemplate>> = _cannedResponses.asStateFlow()

    suspend fun assignOrderToStaff(orderId: String, staffId: String, staffName: String) {
        if (orderId.isBlank()) return
        try {
            firestore.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "assignedStaffName" to staffName,
                        "updatedAt" to System.currentTimeMillis().toString()
                    )
                ).await()

            // Update local workload
            val list = _staffMembers.value.map { staff ->
                if (staff.id == staffId) {
                    val count = staff.activeTasksCount + 1
                    staff.copy(activeTasksCount = count, workloadScore = (count * 20).coerceAtMost(100))
                } else staff
            }
            _staffMembers.value = list
        } catch (_: Exception) {}
    }

    suspend fun addTaskToOrder(orderId: String, title: String, assignedStaff: String, deadline: String) {
        if (orderId.isBlank() || title.isBlank()) return
        try {
            val task = OrderTask(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                title = title,
                assignedStaffName = assignedStaff,
                deadline = deadline,
                isCompleted = false
            )
            val docRef = firestore.collection("orders").document(orderId)
            val snapshot = docRef.get().await()
            val existing = (snapshot.get("tasks") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
            val updated = existing + mapOf(
                "id" to task.id,
                "orderId" to task.orderId,
                "title" to task.title,
                "assignedStaffName" to task.assignedStaffName,
                "deadline" to task.deadline,
                "isCompleted" to task.isCompleted
            )
            docRef.update("tasks", updated).await()
        } catch (_: Exception) {}
    }

    suspend fun toggleTaskCompletion(orderId: String, taskId: String, currentStatus: Boolean) {
        if (orderId.isBlank() || taskId.isBlank()) return
        try {
            val docRef = firestore.collection("orders").document(orderId)
            val snapshot = docRef.get().await()
            val existing = (snapshot.get("tasks") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: return
            val updated = existing.map { map ->
                if (map["id"] == taskId) {
                    map.toMutableMap().apply { put("isCompleted", !currentStatus) }
                } else map
            }
            docRef.update("tasks", updated).await()
        } catch (_: Exception) {}
    }

    companion object {
        fun getInitialStaff(): List<StaffMember> {
            return listOf(
                StaffMember(
                    id = "staff_1",
                    fullName = "Dr. Elmar Məmmədov",
                    email = "elmarmmdov1@gmail.com",
                    roleTitle = "Baş Elmi Redaktor & Superadmin",
                    activeTasksCount = 3,
                    completedTasksCount = 42,
                    workloadScore = 45
                ),
                StaffMember(
                    id = "staff_2",
                    fullName = "Nicat Rzayev",
                    email = "rzayevnicat00@gmail.com",
                    roleTitle = "Sistem İnzibatçısı & Texniki Menecer",
                    activeTasksCount = 2,
                    completedTasksCount = 38,
                    workloadScore = 30
                ),
                StaffMember(
                    id = "staff_3",
                    fullName = "Aygün Qasımova",
                    email = "aygun@bestgroup.az",
                    roleTitle = "İqtisadiyyat & Maliyyə Mütəxəssisi",
                    activeTasksCount = 5,
                    completedTasksCount = 29,
                    workloadScore = 75
                ),
                StaffMember(
                    id = "staff_4",
                    fullName = "Rauf Əliyev",
                    email = "rauf@bestgroup.az",
                    roleTitle = "Hüquq və Beynəlxalq Münasibətlər Redaktoru",
                    activeTasksCount = 4,
                    completedTasksCount = 31,
                    workloadScore = 60
                ),
                StaffMember(
                    id = "staff_5",
                    fullName = "Nigar Həsənli",
                    email = "nigar@bestgroup.az",
                    roleTitle = "Statistik Analiz & SPSS Mütəxəssisi",
                    activeTasksCount = 1,
                    completedTasksCount = 19,
                    workloadScore = 20
                )
            )
        }

        fun getInitialCannedResponses(): List<CannedResponseTemplate> {
            return listOf(
                CannedResponseTemplate(
                    id = "canned_1",
                    title = "Sifariş qəbul olundu",
                    category = "Sifariş",
                    responseText = "Salam! Sifarişiniz qəbul edildi və tədqiqat planına uyğun olaraq müvafiq sahə üzrə akademik mütəxəssisimizə həvalə olundu. Hər bir mərhələdə sənəd qaralamalarını sizə göndərəcəyik."
                ),
                CannedResponseTemplate(
                    id = "canned_2",
                    title = "İlkin variant hazırdır",
                    category = "Sənəd",
                    responseText = "Salam! Sifarişinizin I fəslinin ilkin variantı hazırlandı və sistemə yükləndi. Zəhmət olmasa sənədi nəzərdən keçirin və elmi rəhbərinizin qeydləri olduqda bizə bildirin."
                ),
                CannedResponseTemplate(
                    id = "canned_3",
                    title = "AAK Plagiat yoxlaması",
                    category = "Keyfiyyət",
                    responseText = "Diplom işiniz rəsmi AAK anti-plagiat sistemindən uğurla keçdi. Unikallıq göstəricisi tələb olunan normadan yüksəkdir (82%+). Rəsmi hesabat sənədlərinizə əlavə olundu."
                ),
                CannedResponseTemplate(
                    id = "canned_4",
                    title = "Düzəlişlər qəbul edildi",
                    category = "Redaktə",
                    responseText = "Elmi rəhbərinizin qeyd etdiyi düzəlişlər qəbul olundu. Mütəxəssisimiz tərəfindən 24 saat ərzində düzəlişlər tam pulsuz olaraq yerinə yetiriləcək."
                )
            )
        }
    }
}

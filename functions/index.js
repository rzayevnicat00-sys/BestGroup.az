/**
 * BestGroup.az - Firebase Cloud Functions
 * Handles Idempotent Push Notifications (FCM) & Secure Admin Role Management
 */

const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Helper: Send Push Notification to all active devices of target user(s)
 */
async function sendPushToUsers(userIds, payload) {
  if (!userIds || userIds.length === 0) return;

  const tokens = [];
  for (const uid of userIds) {
    const devicesSnap = await db.collection("users").doc(uid).collection("devices").where("active", "==", true).get();
    devicesSnap.forEach((doc) => {
      const data = doc.data();
      if (data.token) {
        tokens.push(data.token);
      }
    });
  }

  if (tokens.length === 0) return;

  const message = {
    notification: {
      title: payload.title,
      body: payload.body,
    },
    data: payload.data || {},
    tokens: tokens,
  };

  try {
    const response = await messaging.sendEachForMulticast(message);
    // Cleanup invalid tokens if any
    response.responses.forEach(async (resp, idx) => {
      if (!resp.success && resp.error) {
        const errCode = resp.error.code;
        if (
          errCode === "messaging/invalid-registration-token" ||
          errCode === "messaging/registration-token-not-registered"
        ) {
          // Invalidate device token
          const badToken = tokens[idx];
          for (const uid of userIds) {
            const badDevs = await db.collection("users").doc(uid).collection("devices").where("token", "==", badToken).get();
            badDevs.forEach((d) => d.ref.update({ active: false }));
          }
        }
      }
    });
  } catch (err) {
    console.error("Error sending push notification:", err);
  }
}

/**
 * Helper: Find all Admin, Manager, and Operator User IDs
 */
async function getStaffUserIds() {
  const staffIds = [];
  try {
    const usersSnap = await db.collection("users").get();
    for (const doc of usersSnap.docs) {
      try {
        const userRecord = await admin.auth().getUser(doc.id);
        const claims = userRecord.customClaims || {};
        if (
          claims.role === "admin" ||
          claims.role === "manager" ||
          claims.role === "operator" ||
          claims.admin === true ||
          claims.manager === true ||
          claims.operator === true
        ) {
          staffIds.push(doc.id);
        }
      } catch (_) {}
    }
  } catch (e) {
    console.error("Failed to query staff IDs:", e);
  }
  return staffIds;
}

/**
 * A) Customer creates a new order: "orders/{orderId}"
 * -> Send FCM Notification to Admin / Staff devices
 * Idempotent with document processed tracking
 */
exports.onOrderCreated = onDocumentCreated("orders/{orderId}", async (event) => {
  const snap = event.data;
  if (!snap) return;
  const orderData = snap.data();
  const orderId = event.params.orderId;

  // Idempotency check
  if (orderData._notifiedCreated) return;

  const staffIds = await getStaffUserIds();
  const orderNum = orderData.orderNumber || `#BG-2026-${orderId.substring(0, 4)}`;

  const payload = {
    title: "BestGroup.az • Yeni sifariş",
    body: `Yeni sifariş qəbul edildi: ${orderNum}`,
    data: {
      type: "new_order",
      orderId: orderId,
      orderNumber: orderNum,
      click_action: "FLUTTER_NOTIFICATION_CLICK",
    },
  };

  await sendPushToUsers(staffIds, payload);

  // Mark as processed in Firestore
  await snap.ref.set({ _notifiedCreated: true }, { merge: true });
});

/**
 * B & C) New Message in Conversation: "conversations/{conversationId}/messages/{messageId}"
 * -> If customer sends message in support -> notification to Admin/Staff
 * -> If customer sends message in order chat -> notification to Admin/Staff
 * -> If staff sends message -> notification to customer
 */
exports.onMessageCreated = onDocumentCreated("conversations/{conversationId}/messages/{messageId}", async (event) => {
  const snap = event.data;
  if (!snap) return;
  const msgData = snap.data();
  const conversationId = event.params.conversationId;
  const messageId = event.params.messageId;

  if (msgData._notified) return;

  const convSnap = await db.collection("conversations").doc(conversationId).get();
  if (!convSnap.exists) return;
  const convData = convSnap.data();

  const isStaffSender = msgData.senderRole === "staff" || msgData.senderRole === "admin";
  const customerId = convData.customerId;
  const orderId = convData.orderId;

  if (!isStaffSender) {
    // Message from Customer -> Notify Admin/Staff
    const staffIds = await getStaffUserIds();

    if (orderId) {
      // Order Chat Notification
      const orderSnap = await db.collection("orders").doc(orderId).get();
      const orderNum = orderSnap.exists ? (orderSnap.data().orderNumber || `#BG-${orderId}`) : `#BG-${orderId}`;

      const payload = {
        title: "BestGroup.az • Sifariş mesajı",
        body: `${orderNum} sifarişi üzrə yeni mesajınız var.`,
        data: {
          type: "order_message",
          conversationId: conversationId,
          orderId: orderId,
          orderNumber: orderNum,
          userId: msgData.senderId || customerId,
        },
      };
      await sendPushToUsers(staffIds, payload);
    } else {
      // General Customer Support Chat Notification
      const payload = {
        title: "BestGroup.az • Yeni mesaj",
        body: "Müştəri sizə yeni mesaj göndərdi.",
        data: {
          type: "support_message",
          conversationId: conversationId,
          userId: msgData.senderId || customerId,
        },
      };
      await sendPushToUsers(staffIds, payload);
    }
  } else {
    // Message from Staff -> Notify Customer
    if (customerId) {
      const payload = {
        title: "BestGroup.az • Yeni cavab",
        body: msgData.text ? msgData.text.substring(0, 100) : "Kuratorunuzdan yeni mesaj var.",
        data: {
          type: orderId ? "order_message" : "support_message",
          conversationId: conversationId,
          orderId: orderId || "",
        },
      };
      await sendPushToUsers([customerId], payload);
    }
  }

  await snap.ref.set({ _notified: true }, { merge: true });
});

/**
 * D) Order status updated: "orders/{orderId}"
 * -> Send Push Notification to Customer
 */
exports.onOrderStatusUpdated = onDocumentUpdated("orders/{orderId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  const orderId = event.params.orderId;

  if (!before || !after) return;
  if (before.status === after.status) return;

  const customerId = after.userId;
  if (!customerId) return;

  let statusText = "Sifarişinizin statusu yeniləndi.";
  switch ((after.status || "").toLowerCase()) {
    case "pending":
    case "waiting":
      statusText = "Sifarişiniz qəbul üçün gözləyir.";
      break;
    case "accepted":
      statusText = "Sifarişiniz qəbul edildi.";
      break;
    case "in_progress":
      statusText = "Sifarişiniz hazırlanır.";
      break;
    case "ready":
      statusText = "Sifarişiniz hazırdır.";
      break;
    case "cancelled":
      statusText = "Sifarişiniz ləğv edildi.";
      break;
  }

  const payload = {
    title: `BestGroup.az • ${after.orderNumber || "Sifariş"}`,
    body: statusText,
    data: {
      type: "order_status_update",
      orderId: orderId,
      orderNumber: after.orderNumber || "",
      newStatus: after.status,
    },
  };

  await sendPushToUsers([customerId], payload);
});

/**
 * Secure Admin Claims Setting: Callable only by existing Admins
 */
exports.setUserRole = onCall(async (request) => {
  // Check caller claims
  if (!request.auth || !request.auth.token || request.auth.token.role !== "admin") {
    throw new HttpsError("permission-denied", "Yalnız Admin digər istifadəçilərə rol təyin edə bilər.");
  }

  const targetUid = request.data.targetUid;
  const role = request.data.role; // 'admin', 'manager', 'operator', 'customer'

  if (!targetUid || !role) {
    throw new HttpsError("invalid-argument", "targetUid və role vacibdir.");
  }

  const validRoles = ["admin", "manager", "operator", "customer"];
  if (!validRoles.includes(role.toLowerCase())) {
    throw new HttpsError("invalid-argument", "Yolverilməz rol.");
  }

  const claims = { role: role.toLowerCase() };
  if (role === "admin") claims.admin = true;
  if (role === "manager") claims.manager = true;
  if (role === "operator") claims.operator = true;

  await admin.auth().setCustomUserClaims(targetUid, claims);
  await db.collection("users").doc(targetUid).set({ role: role.toLowerCase() }, { merge: true });

  return { success: true, message: `İstifadəçiyə '${role}' rolu təyin edildi.` };
});

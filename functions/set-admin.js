/**
 * BestGroup.az - Master Admin Initialization Script
 * Target Master Admin: elmarmmdov1@gmail.com
 *
 * This script runs strictly in a trusted Node.js / Firebase Admin environment.
 * It searches for the user account by email and applies cryptographic Custom Claims:
 *   - role: 'admin'
 *   - admin: true
 */

const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

const TARGET_ADMIN_EMAIL = "elmarmmdov1@gmail.com";

async function makeUserAdmin() {
  console.log(`[BestGroup Admin Setup] Başlanılır: ${TARGET_ADMIN_EMAIL}`);
  try {
    const userRecord = await admin.auth().getUserByEmail(TARGET_ADMIN_EMAIL);
    const uid = userRecord.uid;
    console.log(`[BestGroup Admin Setup] İstifadəçi tapıldı! UID: ${uid}`);

    const currentClaims = userRecord.customClaims || {};
    console.log(`[BestGroup Admin Setup] Mövcud Claims:`, JSON.stringify(currentClaims));

    const newClaims = {
      ...currentClaims,
      role: "admin",
      admin: true
    };

    await admin.auth().setCustomUserClaims(uid, newClaims);
    console.log(`[BestGroup Admin Setup] Custom Claims uğurla yazıldı:`, JSON.stringify(newClaims));

    // Also sync the Firestore document
    const db = admin.firestore();
    await db.collection("users").doc(uid).set(
      {
        role: "admin",
        email: TARGET_ADMIN_EMAIL,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      },
      { merge: true }
    );
    console.log(`[BestGroup Admin Setup] Firestore 'users/${uid}' sənədi yeniləndi.`);

    console.log(`\n=======================================================`);
    console.log(`UĞURLU: '${TARGET_ADMIN_EMAIL}' artıq sistemdə YALNIZ VƏ YEGANƏ ADMİNDİR!`);
    console.log(`UID: ${uid}`);
    console.log(`Tətbiqdən çıxıb yenidən daxil olmaq kifayətdir.`);
    console.log(`=======================================================\n`);
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      console.error(`\nXƏTA: '${TARGET_ADMIN_EMAIL}' ünvanı ilə Firebase Auth-da hesab tapılmadı!`);
      console.error(`Zəhmət olmasa əvvəlcə tətbiqdən bu email ilə qeydiyyatdan keçin, sonra yenidən icra edin.\n`);
    } else {
      console.error(`[BestGroup Admin Setup] Xəta baş verdi:`, error);
    }
  }
}

makeUserAdmin();

import { onDocumentCreated, onDocumentDeleted } from "firebase-functions/v2/firestore";
import { db } from "./admin";
import { FieldValue } from "firebase-admin/firestore";

/**
 * Maintient le compteur global d'utilisateurs
 */
export const onUserCreated = onDocumentCreated("users/{userId}", async (event) => {
    const statsRef = db.collection("appConfig").doc("stats");
    await statsRef.set({
        totalUsers: FieldValue.increment(1)
    }, { merge: true });
});

export const onUserDeleted = onDocumentDeleted("users/{userId}", async (event) => {
    const statsRef = db.collection("appConfig").doc("stats");
    await statsRef.set({
        totalUsers: FieldValue.increment(-1)
    }, { merge: true });
});

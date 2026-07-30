import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { db } from "./admin";

/**
 * PHOEN-X v9.4.8 - Verrouillage de l'identité officielle
 */
export const lockIdentity = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");
    const { firstName, lastName } = request.data;
    if (!firstName || !lastName) throw new HttpsError("invalid-argument", "Champs incomplets");

    const userRef = db.collection("users").doc(request.auth.uid);

    await userRef.update({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        isIdentityLocked: true,
        identityLockedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true };
});

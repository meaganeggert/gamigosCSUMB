package com.example.gamigosjava.data.repository;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.*;

public class FirestoreUtils {
    private static final int DEFAULT_BATCH_SIZE = 10;

    /** Deletes all documents in a collection in batches. Subcollections are NOT deleted. */

    public static Task<Void> deleteCollection(FirebaseFirestore db, CollectionReference collectionRef, int batchSize) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        deleteCollectionPage(db, collectionRef, batchSize, tcs);
        return tcs.getTask();
    }
    private static void deleteCollectionPage(FirebaseFirestore db,
                                              CollectionReference collectionRef,
                                              int batchSize,
                                              TaskCompletionSource<Void> tcs) {

        Query query = collectionRef.limit(batchSize);

        query.get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                tcs.trySetResult(null);
                return;
            }

            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                batch.delete(doc.getReference());
            }

            batch.commit()
                    .addOnSuccessListener(aVoid ->
                            deleteCollectionPage(db, collectionRef, batchSize, tcs))
                    .addOnFailureListener(tcs::trySetException);

        }).addOnFailureListener(tcs::trySetException);
    }

    /** Convenience overload with a default batch size. */
    private static void deleteCollectionPage(FirebaseFirestore db,
                                        String collectionPath,
                                        TaskCompletionSource<Void> tcs) {
        deleteCollectionPage(db, db.collection(collectionPath), DEFAULT_BATCH_SIZE, tcs);
    }
}

package com.example.gamigosjava.data.repository;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.*;

public class FirestoreUtils {
    private static final int DEFAULT_BATCH_SIZE = 100;

    /** Deletes all documents in a collection in batches. Subcollections are NOT deleted. */
    public static void deleteCollection(FirebaseFirestore db,
                                        CollectionReference collectionRef,
                                        int batchSize,
                                        Runnable onComplete,
                                        OnFailureListener onError) {

        Query query = collectionRef.limit(batchSize);

        query.get().addOnSuccessListener(querySnapshot -> {
            int size = querySnapshot.size();
            if (size == 0) {
                if (onComplete != null) onComplete.run();
                return;
            }

            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                batch.delete(doc.getReference());
            }

            batch.commit()
                    .addOnSuccessListener(aVoid ->
                            deleteCollection(db, collectionRef, batchSize, onComplete, onError))
                    .addOnFailureListener(onError);

        }).addOnFailureListener(onError);
    }

    /** Convenience overload with a default batch size. */
    public static void deleteCollection(FirebaseFirestore db,
                                        String collectionPath,
                                        Runnable onComplete,
                                        OnFailureListener onError) {
        deleteCollection(db, db.collection(collectionPath), DEFAULT_BATCH_SIZE, onComplete, onError);
    }
}

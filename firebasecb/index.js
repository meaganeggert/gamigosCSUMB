/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

const {
  onDocumentCreated,
  onDocumentUpdated,
  onDocumentDeleted,
} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const algoliasearch = require("algoliasearch");

admin.initializeApp();

// ------------------------------------------------------------------
// 1. Algolia config (you hard-coded them; that's fine for now)
const ALGOLIA_APP_ID = "5LHKRX1QE8";
const ALGOLIA_ADMIN_KEY = "27bafb32a5e545782693faf41f76298e";
const ALGOLIA_INDEX_NAME = "users";

// single client / index for all functions
let algoliaIndex = null;
if (ALGOLIA_APP_ID && ALGOLIA_ADMIN_KEY) {
  const client = algoliasearch(ALGOLIA_APP_ID, ALGOLIA_ADMIN_KEY);
  algoliaIndex = client.initIndex(ALGOLIA_INDEX_NAME);
} else {
  console.warn("Algolia config missing");
}

// helper to shape user
function buildAlgoliaUser(uid, data) {
  return {
    objectID: uid,
    displayName: data.displayName || "",
    email: data.email || "",
    photoUrl: data.photoUrl || "",
    privacyLevel: data.privacyLevel || "public",
  };
}

// ------------------------------------------------------------------
// 2. Firestore → Algolia live sync
exports.onUserCreated = onDocumentCreated("users/{uid}", async (event) => {
  if (!algoliaIndex) return;
  const uid = event.params.uid;
  const data = event.data ? event.data.data() : {};
  await algoliaIndex.saveObject(buildAlgoliaUser(uid, data));
});

exports.onUserUpdated = onDocumentUpdated("users/{uid}", async (event) => {
  if (!algoliaIndex) return;
  const uid = event.params.uid;
  const after = event.data.after.data() || {};
  await algoliaIndex.saveObject(buildAlgoliaUser(uid, after));
});

exports.onUserDeleted = onDocumentDeleted("users/{uid}", async (event) => {
  if (!algoliaIndex) return;
  const uid = event.params.uid;
  await algoliaIndex.deleteObject(uid);
});

// ------------------------------------------------------------------
// 3. Backfill endpoint (call once after deploy)
exports.backfillUsers = onRequest(async (req, res) => {
  // make it a POST to avoid accidents
  if (req.method !== "POST") {
    return res.status(405).send("Use POST");
  }

  // simple secret – change this
  const secret = "dev-secret";
  if (req.headers["x-backfill-secret"] !== secret) {
    return res.status(403).send("Forbidden");
  }

  if (!algoliaIndex) {
    return res.status(500).send("Algolia not configured");
  }

  try {
    const snapshot = await admin.firestore().collection("users").get();
    const objects = [];

    snapshot.forEach((doc) => {
      const data = doc.data() || {};
      objects.push({
        objectID: doc.id,
        displayName: data.displayName || "",
        email: data.email || "",
        photoUrl: data.photoUrl || "",
        privacyLevel: data.privacyLevel || "public",
      });
    });

    // batch to Algolia
    await algoliaIndex.saveObjects(objects);
    return res
      .status(200)
      .send(`Backfilled ${objects.length} users to Algolia`);
  } catch (err) {
    console.error("Backfill failed:", err);
    return res.status(500).send(err.message);
  }
});

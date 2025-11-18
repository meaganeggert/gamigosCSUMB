const { onRequest } = require('firebase-functions/v2/https');
const axios = require('axios');
const { SecretManagerServiceClient } = require('@google-cloud/secret-manager');

const admin = require('firebase-admin');
if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();

const sm = new SecretManagerServiceClient();
let cachedToken;

async function getBggToken() {
  if (cachedToken) return cachedToken;
  const [v] = await sm.accessSecretVersion({
    name: `projects/${process.env.GCLOUD_PROJECT}/secrets/BGG_API_TOKEN/versions/latest`,
  });
  cachedToken = v.payload.data.toString();
  return cachedToken;
}

exports.bggSearch = onRequest(
  { region: 'us-central1', cors: true },
  async (req, res) => {
    try {
      const token = await getBggToken();
      const r = await axios.get(
        'https://boardgamegeek.com/xmlapi2/search',
        {
          params: req.query,
          headers: {
            'Authorization': `Bearer ${token}`,
            'User-Agent': 'Gamigos/1.0 (Firebase)',
            'Accept': 'application/xml',
          },
          timeout: 10000,
        },
      );
      res.set('Content-Type', 'application/xml');
      res.set('Cache-Control', 'public, max-age=30, s-maxage=60');
      res.status(r.status).send(r.data);
    } catch (e) {
      const status = (e.response && e.response.status) || 500;
      const body = (e.response && e.response.data) || e.message;
      res.status(status).send(body);
    }
  },
);

exports.bggThing = onRequest(
  { region: 'us-central1', cors: true },
  async (req, res) => {
    try {
      const token = await getBggToken();
      const r = await axios.get(
        'https://boardgamegeek.com/xmlapi2/thing',
        {
          params: req.query,
          headers: {
            'Authorization': `Bearer ${token}`,
            'User-Agent': 'Gamigos/1.0 (Firebase)',
            'Accept': 'application/xml',
          },
          timeout: 10000,
        },
      );
      res.set('Content-Type', 'application/xml');
      res.set('Cache-Control', 'public, max-age=30, s-maxage=60');
      res.status(r.status).send(r.data);
    } catch (e) {
      const status = (e.response && e.response.status) || 500;
      const body = (e.response && e.response.data) || e.message;
      res.status(status).send(body);
    }
  },
);

//exports.backfillAchievementActivities = onRequest(
//  { region: 'us-central1', timeoutSeconds: 540 },   // long enough for big backfills
//  async (req, res) => {
//    try {
//      const usersSnap = await db.collection('users').get();
//      console.log(`Found ${usersSnap.size} users`);
//
//      const batchCommits = [];
//      let batch = db.batch();
//      let opCount = 0;
//
//      // Loop all users
//      for (const userDoc of usersSnap.docs) {
//        const userId = userDoc.id;
//        const userData = userDoc.data();
//        const actorName = userData.displayName || userData.name || 'Someone';
//
//        // Per-user earned achievements: users/{userId}/achievements
//        const userAchSnap = await userDoc.ref.collection('achievements').get();
//        if (userAchSnap.empty) continue;
//
//        for (const achDoc of userAchSnap.docs) {
//          const achId = achDoc.id;
//          const userAchData = achDoc.data();
//
//          // Look up the achievement definition in root /achievements collection
//          const defSnap = await db.collection('achievements').doc(achId).get();
//          if (!defSnap.exists) {
//            console.warn(`No definition for achievement ${achId}, skipping`);
//            continue;
//          }
//
//          const def = defSnap.data();
//          const achievementName = def.name || achId;
//
//          // Build the Activity document
//          const activityRef = db.collection('activities').doc();
//          const message = `${actorName} earned ${achievementName}`;
//
//          const activity = {
//            type: 'ACHIEVEMENT_EARNED',
//            actorId: userId,
//            actorName,
//            targetId: achId,
//            targetName: achievementName,
//            message,
//            visibility: 'friends',
//            createdAt:
//              userAchData.earnedAt ||
//              userAchData.unlockedAt ||
//              admin.firestore.FieldValue.serverTimestamp(),
//          };
//
//          batch.set(activityRef, activity);
//          opCount++;
//
//          // Firestore batch limit: 500 writes
//          if (opCount === 500) {
//            batchCommits.push(batch.commit());
//            batch = db.batch();
//            opCount = 0;
//          }
//        }
//      }
//
//      if (opCount > 0) {
//        batchCommits.push(batch.commit());
//      }
//
//      await Promise.all(batchCommits);
//
//      res.status(200).send('Backfilled ACHIEVEMENT_EARNED activities.');
//    } catch (err) {
//      console.error(err);
//      res.status(500).send(err.toString());
//    }
//  },
//);

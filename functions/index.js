const { onRequest } = require('firebase-functions/v2/https');
const axios = require('axios');
const { SecretManagerServiceClient } = require('@google-cloud/secret-manager');

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
  { region: 'us-central1', cors: true, enforceAppCheck: true },
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
  { region: 'us-central1', cors: true, enforceAppCheck: true },
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

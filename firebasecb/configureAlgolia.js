import algoliasearch from "algoliasearch";

// Replace with your credentials
const ALGOLIA_APP_ID = "5LHKRX1QE8";
const ALGOLIA_ADMIN_KEY = "27bafb32a5e545782693faf41f76298e";
const ALGOLIA_INDEX_NAME = "users";

const client = algoliasearch(ALGOLIA_APP_ID, ALGOLIA_ADMIN_KEY);
const index = client.initIndex(ALGOLIA_INDEX_NAME);

(async () => {
  try {
    await index.setSettings({
      attributesForFaceting: ["privacyLevel"], // Enables filtering by privacy
    });
    console.log("✅ Algolia index settings updated successfully.");
  } catch (err) {
    console.error("❌ Error updating settings:", err);
  }
})();

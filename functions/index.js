const functions = require("firebase-functions");
const admin = require("firebase-admin");
const OneSignal = require("onesignal-node");

admin.initializeApp();

const client = new OneSignal.Client(
  "YOUR_ONESIGNAL_APP_ID",
  "YOUR_ONESIGNAL_REST_API_KEY"
);

exports.sendPushOnMessage = functions.firestore
  .document("chats/{chatId}/messages/{msgId}")
  .onCreate(async (snap) => {

    const msg = snap.data();
    const receiverId = msg.receiverId;

    const userDoc = await admin.firestore()
      .collection("users")
      .doc(receiverId)
      .get();

    const oneSignalId = userDoc.data().oneSignalId;

    if (!oneSignalId) return;

    const notification = {
      contents: { en: msg.text },
      headings: { en: "New Message" },
      include_player_ids: [oneSignalId]
    };

    await client.createNotification(notification);
});

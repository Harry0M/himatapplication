/**
 * Himat Textile — push notifications.
 *
 * The apps do not talk to FCM directly (that would need a server key on the phone). Instead every
 * app writes a small record under `/notifications/<id>` when something happens — a trip starts, a
 * salesman joins, an order is booked. This function picks that record up and pushes it to every
 * registered device except the one that caused it.
 *
 * Who gets it: every device in `/fcm_tokens` whose role is not Sub Agent. Sub Agents have a
 * read-only login scoped to their own customers, so team activity is not their business.
 *
 * Because the message carries a `notification` block, Android shows it even when the app is closed.
 */

const { onValueCreated } = require("firebase-functions/v2/database");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const RTDB_INSTANCE = "himatsms-default-rtdb";
const CHANNEL_ID = "himat_work_updates";

/** FCM accepts at most 500 tokens per send. */
const SEND_CHUNK = 500;

/** Notification records older than this are cleaned up as we go, so the node stays small. */
const HISTORY_TTL_MS = 14 * 24 * 60 * 60 * 1000;

/** Sub Agents are deliberately left out of team notifications. */
function isSubAgent(role) {
  const value = String(role || "").trim().toLowerCase();
  return value === "agent" || value === "sub agent" || value === "subagent";
}

function chunk(list, size) {
  const out = [];
  for (let i = 0; i < list.length; i += size) out.push(list.slice(i, i + size));
  return out;
}

/**
 * Devices that should hear about this event.
 *
 * A device is skipped when it belongs to a Sub Agent, or when it belongs to the person who caused
 * the event — you do not need a notification for your own order. Matching is by employee id, with
 * an email fallback for an owner who has no staff record.
 */
function pickTargets(tokensSnapshot, note) {
  const actorId = Number(note.actorId || 0);
  const actorEmail = String(note.actorEmail || "").trim().toLowerCase();
  const targets = [];

  tokensSnapshot.forEach((child) => {
    const row = child.val() || {};
    const token = String(row.token || child.key || "").trim();
    if (!token) return;
    if (isSubAgent(row.role)) return;

    const sameEmployee = actorId > 0 && Number(row.employeeId || 0) === actorId;
    const sameEmail =
      actorEmail.length > 0 && String(row.email || "").trim().toLowerCase() === actorEmail;
    if (sameEmployee || sameEmail) return;

    targets.push({ key: child.key, token });
  });

  return targets;
}

/** Drops tokens FCM told us no longer exist, so the list does not grow stale forever. */
async function pruneTokens(db, deadKeys) {
  if (deadKeys.length === 0) return;
  const updates = {};
  deadKeys.forEach((key) => {
    updates[key] = null;
  });
  await db.ref("fcm_tokens").update(updates);
  logger.info(`Pruned ${deadKeys.length} dead token(s)`);
}

/** Deletes announcements older than the TTL. Runs opportunistically on each new event. */
async function trimHistory(db) {
  const cutoff = Date.now() - HISTORY_TTL_MS;
  const snapshot = await db
    .ref("notifications")
    .orderByChild("createdAt")
    .endAt(cutoff)
    .limitToFirst(200)
    .get();
  if (!snapshot.exists()) return;
  const updates = {};
  snapshot.forEach((child) => {
    updates[child.key] = null;
  });
  if (Object.keys(updates).length > 0) {
    await db.ref("notifications").update(updates);
    logger.info(`Trimmed ${Object.keys(updates).length} old notification(s)`);
  }
}

exports.pushWorkNotification = onValueCreated(
  { ref: "/notifications/{noteId}", instance: RTDB_INSTANCE, region: "us-central1" },
  async (event) => {
    const note = event.data.val();
    if (!note || typeof note !== "object") {
      logger.warn("notification record was not an object, ignoring");
      return;
    }

    const title = String(note.title || "Himat Textile").slice(0, 120);
    const body = String(note.body || "").slice(0, 300);
    if (!body) {
      logger.warn(`notification ${event.params.noteId} had no body, ignoring`);
      return;
    }

    const db = admin.database();
    const tokensSnapshot = await db.ref("fcm_tokens").get();
    if (!tokensSnapshot.exists()) {
      logger.info("no registered devices yet");
      return;
    }

    const targets = pickTargets(tokensSnapshot, note);
    if (targets.length === 0) {
      logger.info("no devices to notify for this event");
      await trimHistory(db);
      return;
    }

    const message = {
      notification: { title, body },
      data: {
        // The app uses these to open the right screen and to de-duplicate against its own
        // in-app notification for the same event.
        noteId: String(event.params.noteId),
        type: String(note.type || ""),
        refId: String(note.refId || ""),
        actorName: String(note.actorName || ""),
      },
      android: {
        priority: "high",
        notification: {
          channelId: CHANNEL_ID,
          // Same tag as the in-app notification, so the two never stack up
          tag: String(event.params.noteId),
          clickAction: "android.intent.action.MAIN",
        },
      },
    };

    const deadKeys = [];
    let sent = 0;

    for (const group of chunk(targets, SEND_CHUNK)) {
      const response = await admin.messaging().sendEachForMulticast({
        ...message,
        tokens: group.map((t) => t.token),
      });
      sent += response.successCount;
      response.responses.forEach((result, index) => {
        if (result.success) return;
        const code = result.error && result.error.code;
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token" ||
          code === "messaging/invalid-argument"
        ) {
          deadKeys.push(group[index].key);
        } else {
          logger.warn(`send failed for one device: ${code || "unknown error"}`);
        }
      });
    }

    logger.info(`pushed "${title}" to ${sent}/${targets.length} device(s)`);
    await pruneTokens(db, deadKeys);
    await trimHistory(db);
  }
);

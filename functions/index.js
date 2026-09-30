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

const { onValueCreated, onValueWritten } = require("firebase-functions/v2/database");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const RTDB_INSTANCE = "himatsms-default-rtdb";
const CHANNEL_ID = "himat_work_updates";
const REGION = "us-central1";

/** FCM accepts at most 500 tokens per send. */
const SEND_CHUNK = 500;

/** Notification records older than this are cleaned up as we go, so the node stays small. */
const HISTORY_TTL_MS = 14 * 24 * 60 * 60 * 1000;

// -----------------------------------------------------------------------------
// The member index: who is allowed into the database at all
//
// The database rules need to answer "is this signed-in account one of ours?" — but rules cannot
// search a node, so they cannot look through `employees` for a matching email. They need a node keyed
// by something derived from the token.
//
// That node cannot be written by the apps either. That was the original hole: `employees` was
// writable by anyone, and both apps read `role: "Admin"` off it, so any Google account could hand
// itself admin with one REST call. So `members` is written *only here*, mirrored from `employees`,
// and closed to every client. The rules trust it precisely because no client can touch it.
//
// Shape: members/<emailKey> = { employeeId, name, role, active }
// The key is the same sanitised email the apps already use for `super_admins`.
// -----------------------------------------------------------------------------

/** '.' -> '_' and '@' -> '_at_'. Must match Android sanitizeEmail and the web emailKey. */
function emailKey(email) {
  return String(email || "").trim().toLowerCase().replace(/\./g, "_").replace(/@/g, "_at_");
}

/** Both login addresses an employee record can carry. */
function loginEmailsOf(emp) {
  return [emp && emp.email, emp && emp.alternateEmail]
    .map((e) => String(e || "").trim().toLowerCase())
    .filter((e) => e.length > 0 && e.includes("@"));
}

/** A deactivated or suspended staff member stays in the index, marked inactive, so rules deny them. */
function isActiveEmployee(emp) {
  if (!emp) return false;
  if (emp.isDeleted === true || emp.deleted === true) return false;
  if (emp.isBlocked === true || emp.blocked === true) return false;
  const status = String(emp.status || "").trim().toLowerCase();
  if (status === "suspended" || status === "deactivated") return false;
  return true;
}

function memberRowFor(emp, employeeId) {
  return {
    employeeId: Number(employeeId) || 0,
    name: String(emp.name || "").trim(),
    email: loginEmailsOf(emp)[0] || "",
    role: String(emp.role || "Salesman").trim(),
    active: isActiveEmployee(emp),
    updatedAt: Date.now(),
  };
}

/**
 * Keeps `members` in step with `employees`.
 *
 * Fires on create, update and delete. An email moved from one record to another, or removed
 * altogether, has to drop its old key or a departed address would keep its access.
 */
exports.syncMemberIndex = onValueWritten(
  { ref: "/employees/{employeeId}", instance: RTDB_INSTANCE, region: REGION },
  async (event) => {
    const before = event.data.before.val();
    const after = event.data.after.val();
    const employeeId = event.params.employeeId;

    const beforeKeys = before ? loginEmailsOf(before).map(emailKey) : [];
    const afterKeys = after ? loginEmailsOf(after).map(emailKey) : [];

    const updates = {};

    // Addresses this record no longer uses lose their entry, but only if no other record claims it
    for (const key of beforeKeys) {
      if (!afterKeys.includes(key)) updates[key] = null;
    }
    for (const key of afterKeys) {
      updates[key] = memberRowFor(after, employeeId);
    }

    if (Object.keys(updates).length === 0) return;

    const db = admin.database();

    // Guard the removals: another employee record may legitimately hold the same address now
    for (const key of Object.keys(updates)) {
      if (updates[key] !== null) continue;
      const existing = await db.ref(`members/${key}`).get();
      if (existing.exists() && Number(existing.val().employeeId) !== Number(employeeId)) {
        delete updates[key];
      }
    }

    if (Object.keys(updates).length === 0) return;
    await db.ref("members").update(updates);
    logger.info(`member index for employee ${employeeId}: ${JSON.stringify(Object.keys(updates))}`);
  }
);

/**
 * Rebuilds the whole member index from `employees`.
 *
 * Needed once, because `syncMemberIndex` only fires on a change and the staff list already exists.
 * Kept afterwards as a repair tool: it is safe to run at any time and only writes what differs.
 * Owner-only — it is called from the web admin.
 */
exports.rebuildMemberIndex = onSchedule(
  { schedule: "0 4 * * *", timeZone: "Asia/Kolkata", region: REGION },
  async () => {
    const db = admin.database();
    const [empSnap, memberSnap] = await Promise.all([
      db.ref("employees").get(),
      db.ref("members").get(),
    ]);

    const wanted = {};
    if (empSnap.exists()) {
      empSnap.forEach((child) => {
        const emp = child.val() || {};
        loginEmailsOf(emp).forEach((email) => {
          wanted[emailKey(email)] = memberRowFor(emp, emp.id || child.key);
        });
      });
    }

    const updates = {};
    Object.keys(wanted).forEach((key) => {
      const current = memberSnap.child(key).val();
      // updatedAt always differs, so compare only the fields that decide access
      if (
        !current ||
        current.active !== wanted[key].active ||
        String(current.role) !== String(wanted[key].role) ||
        Number(current.employeeId) !== Number(wanted[key].employeeId)
      ) {
        updates[key] = wanted[key];
      }
    });
    if (memberSnap.exists()) {
      memberSnap.forEach((child) => {
        if (!(child.key in wanted)) updates[child.key] = null;
      });
    }

    if (Object.keys(updates).length === 0) {
      logger.info("member index already in step");
      return;
    }
    await db.ref("members").update(updates);
    logger.info(`member index rebuilt: ${Object.keys(updates).length} change(s)`);
  }
);

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

/**
 * Writes one announcement, which `pushWorkNotification` below then delivers.
 *
 * The key is supplied by the caller rather than generated, and that is the whole idempotence story:
 * `pushWorkNotification` triggers on a node being *created*, so writing the same key twice pushes
 * once. Birthdays rely on this — the phone app and the daily job both write
 * `birthday_<customerId>_<yyyymmdd>` and whoever gets there first wins.
 *
 * `actorId` / `actorEmail` are left empty for anything the office itself raises: there is no person
 * to exclude, so everybody hears about it.
 */
async function writeNote(db, { id, type, title, body, refId }) {
  if (!id || !title || !body) return false;
  const ref = db.ref(`notifications/${id}`);
  const existing = await ref.get();
  if (existing.exists()) return false;
  await ref.set({
    id,
    type,
    title: String(title).slice(0, 120),
    body: String(body).slice(0, 300),
    actorId: 0,
    actorName: "",
    actorEmail: "",
    refId: Number(refId || 0),
    createdAt: Date.now(),
  });
  return true;
}

/** "Firm Name (City)" from whatever the registration form happened to fill in. */
function describeRequest(req) {
  const name = String(req.firmName || req.name || "").trim() || "New registration";
  const where = [req.marketArea, req.city].map((v) => String(v || "").trim()).filter(Boolean).join(", ");
  const phone = String(req.phone || "").trim();
  return { name, detail: [where, phone].filter(Boolean).join(" • ") };
}

/**
 * A customer filled in the public registration form.
 *
 * This has to live server-side. The form is used by people who are not signed in, and the database
 * rules only let them create their own request — they cannot write to `notifications`. So the office
 * raises the announcement on their behalf.
 */
exports.notifyCustomerRegistration = onValueCreated(
  { ref: "/customer_registration_requests/{requestId}", instance: RTDB_INSTANCE, region: REGION },
  async (event) => {
    const req = event.data.val();
    if (!req || typeof req !== "object") return;
    const { name, detail } = describeRequest(req);
    const written = await writeNote(admin.database(), {
      id: `customer_request_${event.params.requestId}`,
      type: "customer_request",
      title: `New customer registration: ${name}`,
      body: [detail, "Waiting for approval in Requests"].filter(Boolean).join(" • "),
      refId: 0,
    });
    logger.info(`customer registration ${event.params.requestId}: note ${written ? "written" : "already existed"}`);
  }
);

/** A supplier filled in the public registration form. Same reasoning as above. */
exports.notifySupplierRegistration = onValueCreated(
  { ref: "/supplier_registration_requests/{requestId}", instance: RTDB_INSTANCE, region: REGION },
  async (event) => {
    const req = event.data.val();
    if (!req || typeof req !== "object") return;
    const { name, detail } = describeRequest(req);
    const written = await writeNote(admin.database(), {
      id: `supplier_request_${event.params.requestId}`,
      type: "supplier_request",
      title: `New supplier registration: ${name}`,
      body: [detail, "Waiting for approval in Requests"].filter(Boolean).join(" • "),
      refId: 0,
    });
    logger.info(`supplier registration ${event.params.requestId}: note ${written ? "written" : "already existed"}`);
  }
);

/**
 * Reads a date of birth well enough to spot a birthday. Mirrors Android's `Birthdays.parse`.
 *
 * The field is genuinely mixed: the web form writes `yyyy-MM-dd` because it uses a date input, while
 * the Android master screen is a free text box people fill in as `dd/MM/yyyy`. Anything that cannot
 * be read confidently returns null and that customer simply gets no reminder — a birthday message on
 * the wrong day is worse than none.
 */
function monthDayOf(raw) {
  const parts = String(raw || "").trim().split(/[-/.\s]+/).filter(Boolean);
  if (parts.length < 3) return null;
  const numbers = parts.slice(0, 3).map((p) => Number(p));
  if (numbers.some((n) => !Number.isInteger(n))) return null;

  let day;
  let month;
  if (parts[0].length === 4) {
    month = numbers[1];
    day = numbers[2];
  } else if (parts[2].length === 4) {
    day = numbers[0];
    month = numbers[1];
  } else {
    return null;
  }
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  return { month, day };
}

/**
 * Today's customer birthdays, once a day at 09:00 India time.
 *
 * The app also checks when it opens, which covers the case of this job being unavailable; the shared
 * deterministic note key means running both never doubles up.
 */
exports.dailyBirthdayCheck = onSchedule(
  { schedule: "0 9 * * *", timeZone: "Asia/Kolkata", region: REGION },
  async () => {
    const db = admin.database();
    const snapshot = await db.ref("customers").get();
    if (!snapshot.exists()) {
      logger.info("no customers yet");
      return;
    }

    const now = new Date();
    const local = new Date(now.toLocaleString("en-US", { timeZone: "Asia/Kolkata" }));
    const month = local.getMonth() + 1;
    const day = local.getDate();
    const dateKey = `${local.getFullYear()}${String(month).padStart(2, "0")}${String(day).padStart(2, "0")}`;

    const birthdays = [];
    snapshot.forEach((child) => {
      const c = child.val() || {};
      if (c.isDeleted || c.deleted) return;
      const parsed = monthDayOf(c.dob);
      if (!parsed || parsed.month !== month || parsed.day !== day) return;
      birthdays.push({ id: Number(c.id || child.key || 0), customer: c });
    });

    if (birthdays.length === 0) {
      logger.info("no birthdays today");
      return;
    }

    let written = 0;
    for (const { id, customer } of birthdays) {
      const name = String(customer.firmName || customer.name || "Customer").trim();
      const owner = String(customer.name || "").trim();
      const phone = String(customer.phone || "").trim();
      const body = [owner && owner !== name ? owner : null, phone ? `Call ${phone}` : null]
        .filter(Boolean)
        .join(" • ");
      const ok = await writeNote(db, {
        id: `birthday_${id}_${dateKey}`,
        type: "birthday",
        title: `Birthday today: ${name}`,
        body: body || "Wish them a happy birthday",
        refId: id,
      });
      if (ok) written++;
    }
    logger.info(`birthdays today: ${birthdays.length}, notes written: ${written}`);
  }
);

exports.pushWorkNotification = onValueCreated(
  { ref: "/notifications/{noteId}", instance: RTDB_INSTANCE, region: REGION },
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

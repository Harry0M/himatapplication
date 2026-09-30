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

const { onValueCreated, onValueWritten, onValueDeleted } = require("firebase-functions/v2/database");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

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

// =============================================================================
// THE BIN
//
// Where a record goes when it is really gone. Every hard delete of a business record — an admin
// removing something outright, or approving somebody else's deletion request — leaves a copy here.
//
// Three deliberate design choices, because they are the ones that will look like bugs later:
//
//  1. The apps know nothing about this. It is a side effect of deletion, caught server-side by an
//     onValueDeleted trigger. No app code writes to the bin, no app code waits for it, and a failure
//     here cannot fail a delete. That is what keeps app behaviour independent of the bin — today and
//     for anything added later.
//
//  2. There is no restore, and there is no auto-expiry. Both were asked for explicitly and both are
//     absent on purpose. If you are reading this wondering where the 30-day cleanup went: it was
//     never written. Please do not add one.
//
//  3. Emptying it needs a password that only lives as a hash, server-side, in a node no client can
//     read or write. A password checked in the browser is not a control — anyone can skip the page
//     and call the database directly.
//
// Shape: bin/<collection>/<id> = { collection, itemId, summary, deletedAt, deletedBy..., record }
// =============================================================================

/** Business records worth keeping a copy of. Derived nodes and notifications are not archived. */
const BIN_COLLECTIONS = [
  "customers",
  "suppliers",
  "visits",
  "purchase_entries",
  "products",
  "brands",
  "transporters",
  "markets",
  "cheques_pdc",
  "leads",
  "employees",
];

/** A readable one-liner, so the bin list means something without opening each record. */
function summarise(collection, record) {
  const r = record || {};
  const pick = (...keys) => {
    for (const k of keys) {
      const v = String(r[k] || "").trim();
      if (v) return v;
    }
    return "";
  };
  switch (collection) {
    case "customers":
      return [pick("firmName", "name"), pick("city"), pick("phone")].filter(Boolean).join(" • ");
    case "suppliers":
      return [pick("firmName", "name"), pick("brand"), pick("marketArea", "city")].filter(Boolean).join(" • ");
    case "visits":
      return [pick("visitCode"), pick("customerName"), pick("date")].filter(Boolean).join(" • ");
    case "purchase_entries":
      return [pick("orderNo"), pick("supplierName"), pick("itemCode"), r.pieces ? `${r.pieces} pcs` : ""]
        .filter(Boolean)
        .join(" • ");
    case "products":
      return [pick("productCode"), pick("name"), pick("supplierName")].filter(Boolean).join(" • ");
    case "cheques_pdc":
      return [pick("chequeNo"), pick("partyName"), r.amount ? `INR ${r.amount}` : ""].filter(Boolean).join(" • ");
    case "employees":
      return [pick("employeeId"), pick("name"), pick("role")].filter(Boolean).join(" • ");
    default:
      return [pick("name", "brandName", "transporterName", "marketName", "firmName"), pick("city")]
        .filter(Boolean)
        .join(" • ");
  }
}

/**
 * Copies a deleted record into the bin.
 *
 * The attribution comes off the record itself: a staff soft delete already stamped deletedBy /
 * deletedByEmail / deletedByRole before the admin approved it, and the Android app now stamps the
 * same fields just before an admin's direct delete. Whatever is missing is recorded as unknown rather
 * than guessed at.
 */
function archiveOnDelete(collection) {
  return async (event) => {
    const record = event.data.val();
    if (!record || typeof record !== "object") return;
    const itemId = event.params.id;

    try {
      await admin
        .database()
        .ref(`bin/${collection}/${itemId}`)
        .set({
          collection,
          itemId: Number(itemId) || itemId,
          summary: summarise(collection, record) || `${collection} ${itemId}`,
          deletedAt: Date.now(),
          deletedBy: String(record.deletedBy || "").trim() || "Unknown",
          deletedByEmail: String(record.deletedByEmail || "").trim(),
          deletedByRole: String(record.deletedByRole || "").trim(),
          deletionReason: String(record.deletionReason || "").trim(),
          record,
        });
      logger.info(`bin: archived ${collection}/${itemId}`);
    } catch (e) {
      // Never let this fail a delete. A missing archive entry is a gap in the record, not data loss.
      logger.error(`bin: could not archive ${collection}/${itemId}: ${e.message}`);
    }
  };
}

// One precise trigger per collection rather than a single /{collection}/{id} wildcard: the wildcard
// would also fire on every notification the history trimmer removes, 200 at a time.
BIN_COLLECTIONS.forEach((collection) => {
  exports[`bin_${collection}`] = onValueDeleted(
    { ref: `/${collection}/{id}`, instance: RTDB_INSTANCE, region: REGION },
    archiveOnDelete(collection)
  );
});

// -----------------------------------------------------------------------------
// Emptying the bin
// -----------------------------------------------------------------------------

/** Where the bin password hash lives. No client can read or write this — Admin SDK only. */
const BIN_META = "bin_meta";

const PBKDF2_ROUNDS = 120000;
const MAX_FAILED_ATTEMPTS = 5;
const LOCKOUT_MS = 15 * 60 * 1000;

function hashPassword(password, salt) {
  return crypto.pbkdf2Sync(String(password), salt, PBKDF2_ROUNDS, 32, "sha256").toString("hex");
}

/** '.' -> '_' and '@' -> '_at_', matching the owner keys under super_admins. */
function ownerKeyOf(email) {
  return emailKey(email);
}

/** Throws unless the caller is a signed-in owner. The bin is owners only, end to end. */
async function requireOwner(request) {
  const auth = request.auth;
  if (!auth) throw new HttpsError("unauthenticated", "Please sign in.");
  const email = String(auth.token && auth.token.email ? auth.token.email : "").trim().toLowerCase();
  if (!email) throw new HttpsError("permission-denied", "This action needs a Google account.");

  const db = admin.database();
  const [byEmail, byUid] = await Promise.all([
    db.ref(`super_admins/${ownerKeyOf(email)}`).get(),
    db.ref(`super_admins/${auth.uid}`).get(),
  ]);
  if (!byEmail.exists() && !byUid.exists()) {
    throw new HttpsError("permission-denied", "Only an Admin can use the bin.");
  }
  return { email, uid: auth.uid };
}

async function readBinMeta(db) {
  const snap = await db.ref(BIN_META).get();
  return snap.exists() ? snap.val() : null;
}

/**
 * Sets or changes the bin password. Owner only.
 *
 * There is no default password on purpose: until an owner sets one, the bin simply cannot be emptied.
 * A shipped default is a password everybody knows.
 */
exports.setBinPassword = onCall({ region: REGION }, async (request) => {
  const who = await requireOwner(request);
  const db = admin.database();

  const newPassword = String((request.data && request.data.newPassword) || "");
  const currentPassword = String((request.data && request.data.currentPassword) || "");
  if (newPassword.length < 8) {
    throw new HttpsError("invalid-argument", "The bin password must be at least 8 characters.");
  }

  const meta = await readBinMeta(db);
  if (meta && meta.hash) {
    if (!currentPassword) {
      throw new HttpsError("invalid-argument", "Enter the current bin password to change it.");
    }
    if (hashPassword(currentPassword, meta.salt) !== meta.hash) {
      throw new HttpsError("permission-denied", "The current bin password is wrong.");
    }
  }

  const salt = crypto.randomBytes(16).toString("hex");
  await db.ref(BIN_META).set({
    salt,
    hash: hashPassword(newPassword, salt),
    updatedAt: Date.now(),
    updatedBy: who.email,
    failedAttempts: 0,
    lockedUntil: 0,
  });
  logger.info(`bin password ${meta && meta.hash ? "changed" : "set"} by ${who.email}`);
  return { ok: true, wasSet: Boolean(meta && meta.hash) };
});

/** Whether a password has been set yet, so the web can ask for the right thing. Owner only. */
exports.binStatus = onCall({ region: REGION }, async (request) => {
  await requireOwner(request);
  const db = admin.database();
  const meta = await readBinMeta(db);
  const binSnap = await db.ref("bin").get();

  const counts = {};
  let total = 0;
  if (binSnap.exists()) {
    binSnap.forEach((collection) => {
      const n = collection.numChildren();
      counts[collection.key] = n;
      total += n;
    });
  }
  return {
    passwordSet: Boolean(meta && meta.hash),
    lockedUntil: (meta && meta.lockedUntil) || 0,
    counts,
    total,
  };
});

/**
 * Empties the bin, or one record from it. Owner plus the bin password.
 *
 * Deliberately one-way: what leaves here is not recoverable, which is the whole point of a separate
 * final place. The password is checked here, on the server, because a check in the browser is not a
 * check at all.
 */
exports.emptyBin = onCall({ region: REGION }, async (request) => {
  const who = await requireOwner(request);
  const db = admin.database();

  const meta = await readBinMeta(db);
  if (!meta || !meta.hash) {
    throw new HttpsError("failed-precondition", "Set a bin password first.");
  }
  if (meta.lockedUntil && Date.now() < meta.lockedUntil) {
    const mins = Math.ceil((meta.lockedUntil - Date.now()) / 60000);
    throw new HttpsError("resource-exhausted", `Too many wrong attempts. Try again in ${mins} minute(s).`);
  }

  const password = String((request.data && request.data.password) || "");
  if (hashPassword(password, meta.salt) !== meta.hash) {
    const failed = Number(meta.failedAttempts || 0) + 1;
    const update = { failedAttempts: failed };
    if (failed >= MAX_FAILED_ATTEMPTS) {
      update.lockedUntil = Date.now() + LOCKOUT_MS;
      update.failedAttempts = 0;
    }
    await db.ref(BIN_META).update(update);
    logger.warn(`bin: wrong password from ${who.email} (attempt ${failed})`);
    throw new HttpsError("permission-denied", "That bin password is wrong.");
  }

  await db.ref(BIN_META).update({ failedAttempts: 0, lockedUntil: 0 });

  const collection = String((request.data && request.data.collection) || "").trim();
  const itemId = String((request.data && request.data.itemId) || "").trim();

  if (collection && !BIN_COLLECTIONS.includes(collection)) {
    throw new HttpsError("invalid-argument", "Unknown bin folder.");
  }

  let removed = 0;
  if (collection && itemId) {
    const ref = db.ref(`bin/${collection}/${itemId}`);
    if ((await ref.get()).exists()) {
      await ref.remove();
      removed = 1;
    }
  } else if (collection) {
    const snap = await db.ref(`bin/${collection}`).get();
    removed = snap.exists() ? snap.numChildren() : 0;
    await db.ref(`bin/${collection}`).remove();
  } else {
    const snap = await db.ref("bin").get();
    if (snap.exists()) {
      snap.forEach((c) => {
        removed += c.numChildren();
      });
    }
    await db.ref("bin").remove();
  }

  logger.info(`bin: ${who.email} removed ${removed} record(s) from ${collection || "everything"}`);
  return { ok: true, removed };
});

// =============================================================================
// CREATING PEOPLE
//
// Who may create whom:
//   Admin  ->  Admin, Staff, Sub Agent
//   Staff  ->  Staff, Sub Agent          (never an Admin)
//   Sub Agent -> nobody
//
// This has to live on the server. The database rules make `employees` owner-only to write, because
// the role field on that record is what both apps read to decide who is an admin — when clients could
// write it, anyone could hand themselves the role with a single REST call. So a staff member cannot be
// allowed to write the node directly, and "staff may add staff" can only work through a call that
// checks the requested role before writing.
//
// The staff code is issued here too. Two people adding staff at the same moment used to be able to
// hand out the same code; deciding it in one place removes the race entirely.
// =============================================================================

const ROLE_ADMIN = "Admin";
const ROLE_STAFF = "Salesman"; // stored value, shown as "Staff"
const ROLE_AGENT = "Agent";

/** Normalises whatever the caller sent into one of the three stored role values, or null. */
function normaliseRole(raw) {
  const v = String(raw || "").trim().toLowerCase();
  if (v === "admin") return ROLE_ADMIN;
  if (v === "salesman" || v === "staff") return ROLE_STAFF;
  if (v === "agent" || v === "sub agent" || v === "subagent") return ROLE_AGENT;
  return null;
}

function creatableRoles(callerRole, isOwner) {
  if (isOwner || callerRole === ROLE_ADMIN) return [ROLE_ADMIN, ROLE_STAFF, ROLE_AGENT];
  if (callerRole === ROLE_STAFF) return [ROLE_STAFF, ROLE_AGENT];
  return [];
}

/** Who is calling, and what the office says they are. */
async function identifyCaller(request) {
  const auth = request.auth;
  if (!auth) throw new HttpsError("unauthenticated", "Please sign in.");
  const email = String(auth.token && auth.token.email ? auth.token.email : "").trim().toLowerCase();
  if (!email) throw new HttpsError("permission-denied", "This action needs a Google account.");

  const db = admin.database();
  const [byEmail, byUid, member] = await Promise.all([
    db.ref(`super_admins/${emailKey(email)}`).get(),
    db.ref(`super_admins/${auth.uid}`).get(),
    db.ref(`members/${emailKey(email)}`).get(),
  ]);

  const isOwner = byEmail.exists() || byUid.exists();
  const memberRow = member.exists() ? member.val() : null;
  if (!isOwner && (!memberRow || memberRow.active !== true)) {
    throw new HttpsError("permission-denied", "This account is not on the staff list.");
  }
  return {
    email,
    uid: auth.uid,
    isOwner,
    role: normaliseRole(memberRow && memberRow.role) || (isOwner ? ROLE_ADMIN : ROLE_STAFF),
    employeeId: Number((memberRow && memberRow.employeeId) || 0),
  };
}

/** Highest staff code in use for a prefix, counting deactivated people so a code is never reused. */
function nextStaffCode(employeesSnapshot, prefix) {
  const used = new Set();
  if (employeesSnapshot.exists()) {
    employeesSnapshot.forEach((child) => {
      const raw = String((child.val() || {}).employeeId || "").trim();
      if (!raw.toUpperCase().startsWith(prefix)) return;
      const n = Number(raw.slice(prefix.length).replace(/^[-_\s]+/, ""));
      // A number this big is a record id that leaked into the field, not a real code
      if (Number.isInteger(n) && n > 0 && n <= 9999) used.add(n);
    });
  }
  let candidate = (used.size ? Math.max(...used) : 0) + 1;
  while (used.has(candidate)) candidate++;
  return `${prefix}-${candidate < 10 ? `0${candidate}` : candidate}`;
}

/** epochMillis * 1000 + random, same scheme as the Android IdGenerator and the web newId. */
function newRecordId() {
  return Date.now() * 1000 + Math.floor(Math.random() * 1000);
}

const TEXT_FIELDS = [
  "name", "phone", "phone2", "phone3", "phone4", "phone5", "email", "alternateEmail",
  "firmName", "city", "notes", "address", "currentAddress", "permanentAddress",
  "personalLocation", "emergencyContactName", "emergencyContactPhone", "referredBy",
  "referredByType", "assignedMarkets", "markets", "photoUri", "employeeId",
];

/**
 * Creates a staff member or a Sub Agent, with the role checked against who is asking.
 *
 * Only an owner may change an existing record: editing somebody else's staff row means being able to
 * change the email it is bound to, which is the same thing as handing out access.
 */
exports.saveTeamMember = onCall({ region: REGION }, async (request) => {
  const caller = await identifyCaller(request);
  const db = admin.database();
  const data = (request && request.data) || {};

  const wantedRole = normaliseRole(data.role);
  if (!wantedRole) throw new HttpsError("invalid-argument", "Pick a role.");

  const allowed = creatableRoles(caller.role, caller.isOwner);
  if (allowed.length === 0) {
    throw new HttpsError("permission-denied", "Sub Agent logins cannot add people.");
  }
  if (!allowed.includes(wantedRole)) {
    throw new HttpsError(
      "permission-denied",
      wantedRole === ROLE_ADMIN
        ? "Only an Admin can make somebody an Admin."
        : "You cannot create that role."
    );
  }

  const name = String(data.name || "").trim();
  if (!name) throw new HttpsError("invalid-argument", "Enter a name.");

  const existingId = Number(data.id || 0);
  const isUpdate = existingId > 0;
  if (isUpdate && !caller.isOwner && caller.role !== ROLE_ADMIN) {
    throw new HttpsError("permission-denied", "Only an Admin can change an existing person's record.");
  }

  const employeesSnap = await db.ref("employees").get();

  // A login email is what actually grants access, so only an Admin may set or change one
  let email = String(data.email || "").trim().toLowerCase();
  let alternateEmail = String(data.alternateEmail || "").trim().toLowerCase();
  if (!caller.isOwner && caller.role !== ROLE_ADMIN) {
    if (email || alternateEmail) {
      throw new HttpsError(
        "permission-denied",
        "Only an Admin can give somebody a login email. Add the person now and ask an Admin to link it."
      );
    }
    email = "";
    alternateEmail = "";
  }

  // The same address must not open two records
  if (email) {
    let clash = null;
    employeesSnap.forEach((child) => {
      const row = child.val() || {};
      const ids = [row.email, row.alternateEmail].map((e) => String(e || "").trim().toLowerCase());
      if (ids.includes(email) && Number(child.key) !== existingId) clash = row.name || child.key;
    });
    if (clash) throw new HttpsError("already-exists", `${email} is already used by ${clash}.`);
  }

  const id = isUpdate ? existingId : newRecordId();
  const prefix = wantedRole === ROLE_AGENT ? "AGT" : "EMP";

  const previous = isUpdate ? employeesSnap.child(String(id)).val() || {} : {};
  const record = { ...previous, id, role: wantedRole };

  TEXT_FIELDS.forEach((key) => {
    if (data[key] !== undefined) record[key] = String(data[key] || "").trim();
  });
  record.name = name;
  record.email = email;
  record.alternateEmail = alternateEmail;

  // Keep a code the caller supplied only if it is free; otherwise issue the next one
  const wantedCode = String(record.employeeId || "").trim();
  let codeTaken = false;
  if (wantedCode) {
    employeesSnap.forEach((child) => {
      const row = child.val() || {};
      if (String(row.employeeId || "").trim().toLowerCase() === wantedCode.toLowerCase() &&
        Number(child.key) !== id) {
        codeTaken = true;
      }
    });
  }
  if (!wantedCode || codeTaken) record.employeeId = nextStaffCode(employeesSnap, prefix);

  if (!isUpdate) {
    record.status = "Active";
    record.isBlocked = false;
    record.isDeleted = false;
    record.createdAt = Date.now();
    record.createdBy = caller.email;
  }

  await db.ref(`employees/${id}`).set(record);
  logger.info(`saveTeamMember: ${caller.email} (${caller.role}) ${isUpdate ? "updated" : "created"} ${wantedRole} ${id}`);

  return {
    ok: true,
    id,
    employeeId: record.employeeId,
    role: wantedRole,
    codeChanged: Boolean(wantedCode) && wantedCode !== record.employeeId,
  };
});

/** What the signed-in person may create, so the form only offers those. */
exports.myCreatableRoles = onCall({ region: REGION }, async (request) => {
  const caller = await identifyCaller(request);
  return {
    role: caller.role,
    isOwner: caller.isOwner,
    canCreate: creatableRoles(caller.role, caller.isOwner),
    canSetLoginEmail: caller.isOwner || caller.role === ROLE_ADMIN,
    canEditExisting: caller.isOwner || caller.role === ROLE_ADMIN,
  };
});

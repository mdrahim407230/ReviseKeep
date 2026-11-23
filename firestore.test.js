const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any ReviseKeep collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      roomId: 1,
      title: "Physics",
      subjectTag: "Science",
      colorIndex: 0,
      pinned: false,
      clientCreatedAt: 1000,
      clientUpdatedAt: 1000,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Owner can list empty user-scoped subcollections without permission error", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("entries").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("milestones").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("future_letters").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("activities").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("profile").get());
});

test("Owner create, partial update, and cross-user isolation for user meta, why_profile, notes, entries, milestones, letters, activities", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // 1. User meta
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Ayesha",
      email: "ayesha@example.com",
      currentStreak: 3,
      bestStreak: 5,
      lastRevisionDayKey: "2026-11-22",
      aiStylePrompt: "Speak gently in Bengali",
      sendFullNoteToAi: true,
      encryptedAiKey: "RK_CLOUD_V1:0011:2233:4455",
      cachedModelId: "gemini-3-flash-preview",
      language: "BN",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      currentStreak: 4,
      bestStreak: 5,
      lastRevisionDayKey: "2026-11-23",
      aiStylePrompt: "Updated custom instruction",
      updatedAt: updated,
    })
  );
  // Reject invalid language value
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      language: "INVALID",
      updatedAt: updated,
    })
  );
  // Cross-user read & write blocked
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("notes").get());

  // 2. Why profile
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("profile").doc("why_profile").set({
      userId: ALICE_UID,
      studentName: "Ayesha",
      whyText: "To become self-reliant",
      whoForText: "Amma & Abba",
      dreamText: "Doctor",
      clientUpdatedAt: 1700000000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // 3. Note create & partial update (createdAt preserved)
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      roomId: 1,
      title: "Physics",
      subjectTag: "HSC",
      colorIndex: 1,
      pinned: true,
      clientCreatedAt: 1700000000,
      clientUpdatedAt: 1700000000,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").update({
      title: "Physics Paper 1",
      updatedAt: updated,
    })
  );

  // Reject createdAt tampering & shadow ghost field
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").update({
      createdAt: updated,
      updatedAt: updated,
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("notes").doc("note_2").set({
      userId: ALICE_UID,
      roomId: 2,
      title: "Chemistry",
      subjectTag: "",
      colorIndex: 0,
      pinned: false,
      clientCreatedAt: 1700000000,
      clientUpdatedAt: 1700000000,
      ghostField: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // 4. Entry
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("entries").doc("entry_1").set({
      userId: ALICE_UID,
      roomId: 1,
      noteRoomId: 1,
      contentText: "Newton's Second Law F=ma",
      clientCreatedAt: 1700000000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // 5. Milestone (omitted completedAt -> then updated with completedAt)
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("milestones").doc("ms_1").set({
      userId: ALICE_UID,
      roomId: 1,
      entryRoomId: 1,
      noteRoomId: 1,
      stageIndex: 0,
      status: "ACTIVE_DUE",
      unlockedAt: 1700000000,
      dueAt: 1700007200,
      missedReminderCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("milestones").doc("ms_1").update({
      status: "COMPLETED",
      completedAt: 1700008000,
      updatedAt: updated,
    })
  );
  // Reject invalid stageIndex > 15 or invalid status
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("milestones").doc("ms_bad").set({
      userId: ALICE_UID,
      roomId: 2,
      entryRoomId: 1,
      noteRoomId: 1,
      stageIndex: 99,
      status: "ACTIVE_DUE",
      unlockedAt: 1700000000,
      dueAt: 1700007200,
      missedReminderCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // 6. Future letter & activity
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("future_letters").doc("letter_1").set({
      userId: ALICE_UID,
      roomId: 1,
      unlockStreakDays: 3,
      title: "Keep going",
      bodyText: "Proud of you for revising 3 days straight.",
      clientCreatedAt: 1700000000,
      unlocked: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("activities").doc("2026-11-22").set({
      userId: ALICE_UID,
      dayKey: "2026-11-22",
      completedCount: 4,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Tampered future timestamp rejected
  const future = new Date(Date.now() + 60000);
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("activities").doc("2026-11-23").set({
      userId: ALICE_UID,
      dayKey: "2026-11-23",
      completedCount: 1,
      createdAt: future,
      updatedAt: future,
    })
  );
});

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

function nowTs() {
  return new Date(Date.now() - 2000);
}

function validScheduleBlock(uid) {
  return {
    userId: uid,
    title: "Morning Deep Focus",
    subtitle: "Studio Desk",
    startMinutes: 540,
    durationMinutes: 90,
    category: "focus",
    colorHex: "#E06B3E",
    daysOfWeek: ["MON", "TUE", "WED", "THU", "FRI"],
    chimeEnabled: true,
    completedDateKey: "",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validDisplaySettings(uid) {
  return {
    userId: uid,
    activeKlydoId: "bauhaus_orbit",
    autoCycleArt: true,
    cycleIntervalMinutes: 60,
    pendulumStyle: "brass",
    pendulumEnabled: true,
    hourlyChimeEnabled: true,
    blockChimeEnabled: true,
    keepScreenOn: true,
    nightModeEnabled: true,
    nightStartMinutes: 1320,
    nightEndMinutes: 420,
    clockFormat24h: false,
    showFrame: true,
    favoriteKlydoIds: ["bauhaus_orbit"],
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

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

test("Unauthenticated caller: cannot read or write schedule_blocks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/schedule_blocks`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/schedule_blocks`).doc("b1").set(validScheduleBlock(ALICE_UID))
  );
});

test("Owner: can create, read, update, and delete own schedule_block", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection(`users/${ALICE_UID}/schedule_blocks`).doc("block_1");
  await assertSucceeds(docRef.set(validScheduleBlock(ALICE_UID)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/schedule_blocks`).get());
  await assertSucceeds(docRef.update({ completedDateKey: "2026-11-13", updatedAt: nowTs() }));
  await assertSucceeds(docRef.delete());
});

test("Cross-user isolation: Bob cannot read or write Alice's schedule_blocks or display_settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/schedule_blocks`).doc("block_1").set(validScheduleBlock(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/display_settings`).doc("default").set(validDisplaySettings(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/schedule_blocks`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/schedule_blocks`).doc("block_1").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/display_settings`).doc("default").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/schedule_blocks`).doc("block_2").set(validScheduleBlock(BOB_UID))
  );
});

test("Schema & Red Team: rejects ghost fields, invalid durations, and spoofed userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection(`users/${ALICE_UID}/schedule_blocks`);

  // Ghost field rejected
  await assertFails(col.doc("bad_ghost").set({ ...validScheduleBlock(ALICE_UID), isAdmin: true }));

  // Spoofed userId rejected
  await assertFails(col.doc("bad_uid").set(validScheduleBlock(BOB_UID)));

  // Out-of-bounds duration rejected
  await assertFails(col.doc("bad_dur").set({ ...validScheduleBlock(ALICE_UID), durationMinutes: 2 }));

  // Invalid colorHex rejected
  await assertFails(col.doc("bad_color").set({ ...validScheduleBlock(ALICE_UID), colorHex: "red" }));
});

test("Owner: can create and update display_settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const settingsRef = aliceDb.collection(`users/${ALICE_UID}/display_settings`).doc("default");
  await assertSucceeds(settingsRef.set(validDisplaySettings(ALICE_UID)));
  await assertSucceeds(settingsRef.update({ activeKlydoId: "celestial_observatory", updatedAt: nowTs() }));
  await assertSucceeds(settingsRef.update({ showFrame: false, updatedAt: nowTs() }));
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/display_settings`).doc("bad_frame").set({
      ...validDisplaySettings(ALICE_UID),
      showFrame: "no",
    })
  );
  await assertFails(settingsRef.update({ userId: BOB_UID, updatedAt: nowTs() }));
});

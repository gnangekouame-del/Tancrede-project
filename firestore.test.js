const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_treasurer";
const BOB_UID = "bob_treasurer";

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

function validTransactionPayload(userId) {
  return {
    userId,
    type: "RECETTE",
    amount: 50000,
    category: "DIME",
    categoryLabel: "Dîme",
    paymentMethod: "ESPECES",
    serviceType: "CULTE_DIMANCHE",
    donorOrRecipient: "Fidèle Anonyme",
    note: "Dîme du culte de louange",
    referenceNumber: "D-2026-001",
    date: "2026-10-25",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

test("Unauthenticated user: cannot read transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Unauthenticated user: cannot write transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set(validTransactionPayload(ALICE_UID))
  );
});

test("Authenticated user: cannot read another user's transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("transactions")
      .doc("tx_bob_1")
      .set(validTransactionPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("transactions").doc("tx_bob_1").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("transactions").get()
  );
});

test("Authenticated user: can create valid transaction in their own space", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_alice_1")
      .set(validTransactionPayload(ALICE_UID))
  );
});

test("Authenticated user: cannot create transaction with invalid negative amount", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const invalidPayload = {
    ...validTransactionPayload(ALICE_UID),
    amount: -500,
  };
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_alice_bad")
      .set(invalidPayload)
  );
});

test("Authenticated user: cannot alter immutable fields on update", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_alice_1")
      .set(validTransactionPayload(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Changing userId or createdAt must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_alice_1")
      .update({ userId: BOB_UID })
  );

  // Updating allowed fields like note, amount, updatedAt must succeed
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_alice_1")
      .update({
        amount: 60000,
        note: "Montant ajusté après comptage",
        updatedAt: new Date(),
      })
  );
});

test("Authenticated user: can delete their own transaction", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_alice_1")
      .set(validTransactionPayload(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_alice_1")
      .delete()
  );
});

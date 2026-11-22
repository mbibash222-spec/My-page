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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, now) {
  return {
    userId: uid,
    displayName: "Sanu Manager",
    email: "manager@sanushop.com",
    agentMode: true,
    agentBadgeId: "AGT-104",
    storeBranch: "Sanu Flagship Store",
    seeded: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validProduct(uid, now) {
  return {
    userId: uid,
    sku: "SNU-SH-001",
    name: "Aether Court Sneaker",
    brand: "Sanu Atelier",
    category: "SHOES",
    subcategory: "Sneakers",
    description: "Full-grain cream leather with terracotta suede trim.",
    price: 128.0,
    costPrice: 54.0,
    stockCount: 24,
    lowStockThreshold: 6,
    warehouseBin: "AISLE-A · BIN-04",
    qrPayload: "SANU:SNU-SH-001",
    imageKey: "sneaker",
    sizes: ["US 7", "US 8", "US 9", "US 10", "US 11"],
    colorway: "Alabaster / Terracotta",
    rating: 4.8,
    soldCount: 42,
    featured: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validOrder(uid, now, status = "PLACED") {
  return {
    userId: uid,
    orderNumber: "ORD-2026-8821",
    customerName: "Alice Rivera",
    shippingAddress: "742 Evergreen Terrace, Suite 4B",
    itemsSummary: "Aether Court Sneaker (US 9) x1",
    primaryCategory: "SHOES",
    totalUnits: 1,
    subtotal: 128.0,
    tax: 10.24,
    totalAmount: 138.24,
    estimatedProfit: 74.0,
    paymentMethod: "CARD",
    paymentReference: "PAY-VISA-4242-991A",
    paymentStatus: "PAID",
    trackingStatus: status,
    trackingNote: "Order confirmed and queued for warehouse pick.",
    assignedAgent: "AGT-104",
    etaText: "Est. Tomorrow, 4:00 PM",
    createdAt: now,
    updatedAt: now,
  };
}

function validNotification(uid, now) {
  return {
    userId: uid,
    title: "Order Confirmed",
    message: "ORD-2026-8821 payment verified via VISA •••• 4242.",
    notifType: "ORDER",
    readFlag: false,
    referenceId: "ord_1",
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("products").doc("p1").set(validProduct(ALICE_UID, now))
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's shop data", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, created));
    await db.collection("users").doc(ALICE_UID).collection("products").doc("p1").set(validProduct(ALICE_UID, created));
    await db.collection("users").doc(ALICE_UID).collection("orders").doc("o1").set(validOrder(ALICE_UID, created));
    await db.collection("users").doc(ALICE_UID).collection("notifications").doc("n1").set(validNotification(ALICE_UID, created));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("products").doc("p1").update({ stockCount: 0, updatedAt: new Date(Date.now() - 500) })
  );
});

test("Owner profile create & partial update succeed; ghost fields & createdAt tampering fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      agentMode: false,
      agentBadgeId: "",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Product CRUD (create, get, list, update, delete) and invalid/cross-user rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const created = new Date(Date.now() - 5000);
  const prodRef = aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("sku_sh_001");

  // Empty list query succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("products").get());

  // Valid create (no @DocumentId `id` field)
  await assertSucceeds(prodRef.set(validProduct(ALICE_UID, created)));

  // Single-doc get succeeds for owner, fails for other users & unauthenticated
  await assertSucceeds(prodRef.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("sku_sh_001").get());

  // Partial stock/soldCount update
  await assertSucceeds(
    prodRef.update({
      stockCount: 23,
      soldCount: 43,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Full editable-fields update (matching Product.toUpdateMap())
  await assertSucceeds(
    prodRef.update({
      sku: "SNU-SH-001",
      name: "Aether Court Sneaker v2",
      brand: "Sanu Atelier",
      category: "SHOES",
      subcategory: "Sneakers",
      description: "Updated outsole traction.",
      price: 134.0,
      costPrice: 54.0,
      stockCount: 25,
      lowStockThreshold: 6,
      warehouseBin: "AISLE-A · BIN-05",
      qrPayload: "SANU:SNU-SH-001",
      imageKey: "sneaker",
      sizes: ["US 8", "US 9", "US 10"],
      colorway: "Alabaster / Terracotta",
      rating: 4.9,
      soldCount: 43,
      featured: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId `id` key inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("sku_sh_002").set({
      ...validProduct(ALICE_UID, created),
      id: "sku_sh_002",
    })
  );

  // Reject negative price or invalid category
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("sku_sh_003").set({
      ...validProduct(ALICE_UID, created),
      category: "HATS",
    })
  );

  // Cross-user and unauthenticated delete fail; owner delete succeeds
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("products").doc("sku_sh_001").delete()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("products").doc("sku_sh_001").delete()
  );
  await assertSucceeds(prodRef.delete());
});

test("Order creation, tracking status progression, and terminal DELIVERED lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").set(validOrder(ALICE_UID, created, "PLACED"))
  );

  // Advance to DELIVERED
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").update({
      trackingStatus: "DELIVERED",
      trackingNote: "Delivered to customer doorstep.",
      etaText: "Delivered",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: subsequent updates after DELIVERED must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").update({
      trackingStatus: "PLACED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Notification create, mark read, and future timestamp rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n1").set(validNotification(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n1").update({
      readFlag: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n2").set(
      validNotification(ALICE_UID, new Date(Date.now() + 60000))
    )
  );
});

/**
 * Firestore security rules, tested against the emulator.
 *
 * These rules are the only thing standing between a signed-in user and every other user's
 * finances. A rule that looks right in the console and denies nothing is the failure mode worth
 * fearing, so each rule is asserted from both sides: the owner can, and someone else cannot.
 *
 * Run: npm --prefix firebase test
 */

const assert = require('node:assert');
const { describe, it, before, after, beforeEach } = require('node:test');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const {
  doc,
  getDoc,
  setDoc,
  deleteDoc,
  serverTimestamp,
} = require('firebase/firestore');
const fs = require('node:fs');
const path = require('node:path');

let testEnv;

const OWNER = 'user_owner';
const INTRUDER = 'user_intruder';

/** A record document that satisfies every shape rule, so tests can vary one thing at a time. */
function record(overrides = {}) {
  return {
    tableName: 'TransactionEntity',
    rowId: 't1',
    updatedAt: 1700000000000,
    deviceId: 'device-a',
    payload: '{"id":"t1"}',
    seq: serverTimestamp(),
    ...overrides,
  };
}

function tombstone(overrides = {}) {
  return {
    tableName: 'TransactionEntity',
    rowId: 't1',
    deletedAt: 1700000000000,
    deviceId: 'device-a',
    seq: serverTimestamp(),
    ...overrides,
  };
}

const recordRef = (db, uid = OWNER) => doc(db, `users/${uid}/records/TransactionEntity__t1`);
const tombstoneRef = (db, uid = OWNER) => doc(db, `users/${uid}/tombstones/TransactionEntity__t1`);

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'budgetmaster-rules-test',
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: 8085,
    },
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

describe('ownership', () => {
  it('lets a user write and read their own records', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertSucceeds(setDoc(recordRef(db), record()));
    await assertSucceeds(getDoc(recordRef(db)));
  });

  it('stops a signed-in user reading someone else’s records', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(recordRef(ctx.firestore()), { ...record(), seq: new Date() });
    });
    const db = testEnv.authenticatedContext(INTRUDER).firestore();
    await assertFails(getDoc(recordRef(db, OWNER)));
  });

  it('stops a signed-in user writing into someone else’s subtree', async () => {
    const db = testEnv.authenticatedContext(INTRUDER).firestore();
    await assertFails(setDoc(recordRef(db, OWNER), record()));
  });

  it('stops an unauthenticated client entirely', async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(getDoc(recordRef(db)));
    await assertFails(setDoc(recordRef(db), record()));
  });
});

describe('the sequence must come from the server', () => {
  // This is the rule that stops one device — buggy or hostile — from stranding every other
  // device. Pull cursors advance on `seq`; a value years in the future makes everything behind it
  // invisible, permanently and silently.
  it('rejects a client-chosen sequence', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertFails(setDoc(recordRef(db), record({ seq: new Date(4102444800000) })));
  });

  it('rejects a missing sequence', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    const { seq, ...withoutSeq } = record();
    await assertFails(setDoc(recordRef(db), withoutSeq));
  });

  it('accepts the server sentinel', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertSucceeds(setDoc(recordRef(db), record({ seq: serverTimestamp() })));
  });
});

describe('document shape', () => {
  it('rejects a table the app does not sync', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertFails(setDoc(recordRef(db), record({ tableName: 'ImportedMessageEntity' })));
  });

  it('rejects unknown fields', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertFails(setDoc(recordRef(db), { ...record(), smuggled: 'x' }));
  });

  it('rejects a payload past the size ceiling', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertFails(setDoc(recordRef(db), record({ payload: 'x'.repeat(100001) })));
  });

  it('rejects wrongly typed fields', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertFails(setDoc(recordRef(db), record({ updatedAt: 'yesterday' })));
    await assertFails(setDoc(recordRef(db), record({ rowId: '' })));
  });
});

describe('tombstones', () => {
  it('lets the owner write and delete their own', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertSucceeds(setDoc(tombstoneRef(db), tombstone()));
    await assertSucceeds(deleteDoc(tombstoneRef(db)));
  });

  it('stops anyone else writing one', async () => {
    // Worth its own test: a forged tombstone is a remote delete, so a gap here is not a leak but
    // an erasure of someone else's data.
    const db = testEnv.authenticatedContext(INTRUDER).firestore();
    await assertFails(setDoc(tombstoneRef(db, OWNER), tombstone()));
  });

  it('rejects a client-chosen sequence', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertFails(setDoc(tombstoneRef(db), tombstone({ seq: new Date(4102444800000) })));
  });
});

describe('everything outside the user subtree', () => {
  it('is closed even to the owner', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertFails(setDoc(doc(db, 'anythingElse/x'), { a: 1 }));
    await assertFails(getDoc(doc(db, 'anythingElse/x')));
  });

  it('is closed for a collection nobody has written rules for yet', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertFails(setDoc(doc(db, `users/${OWNER}/somethingNew/x`), { a: 1 }));
  });
});

describe('record deletion', () => {
  it('is allowed for the owner, since removing a row removes its record', async () => {
    const db = testEnv.authenticatedContext(OWNER).firestore();
    await assertSucceeds(setDoc(recordRef(db), record()));
    await assertSucceeds(deleteDoc(recordRef(db)));
  });

  it('is refused for anyone else', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(recordRef(ctx.firestore()), { ...record(), seq: new Date() });
    });
    const db = testEnv.authenticatedContext(INTRUDER).firestore();
    await assertFails(deleteDoc(recordRef(db, OWNER)));
  });
});

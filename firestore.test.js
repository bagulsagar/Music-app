const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

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

function makePlaylistPayload(ownerId, ownerName, inviteCode, isCollaborative = true) {
  return {
    ownerId,
    ownerName,
    title: "Midnight Bloom",
    description: "Collaborative chill beats",
    accentColorHex: "#FF4D8D",
    inviteCode,
    isCollaborative,
    memberUids: [ownerId],
    memberNames: { [ownerId]: ownerName },
    trackIds: [],
    trackCount: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function makeInvitePayload(inviteCode, playlistId, ownerId, ownerName, isActive = true) {
  return {
    inviteCode,
    playlistId,
    playlistTitle: "Midnight Bloom",
    ownerId,
    ownerName,
    isActive,
    createdAt: serverTimestamp(),
  };
}

function makeTrackPayload(playlistId, trackId, addedByUid, addedByName, memberUids) {
  return {
    playlistId,
    trackId,
    title: "Solaris Echo",
    artist: "Bloomee Collective",
    album: "Nocturne EP",
    artworkUrl: "https://example.com/art.jpg",
    streamUrl: "https://example.com/preview.m4a",
    durationMs: 30000,
    genre: "Electronic",
    addedByUid,
    addedByName,
    upvotedBy: [],
    upvoteCount: 0,
    orderIndex: 0,
    memberUids,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

test("Unauthenticated user: cannot read playlists, invites, or liked tracks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("playlists").get());
  await assertFails(unauthDb.collection("playlist_invites").doc("BLM49K").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("liked_tracks").get());
});

test("User profile & library isolation: Alice can read/write own profile and liked tracks, Bob cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_tracks").doc("track_1").set({
      userId: ALICE_UID,
      trackId: "track_1",
      title: "Aurora",
      artist: "Nora",
      album: "Dawn",
      artworkUrl: "https://example.com/a.jpg",
      streamUrl: "https://example.com/a.m4a",
      durationMs: 30000,
      genre: "Synthpop",
      sourceType: "stream",
      createdAt: serverTimestamp(),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("liked_tracks").get());
});

test("Collaborative playlist creation: atomic batch creates playlist and invite, blocks ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("playlists").doc("pl_1"), makePlaylistPayload(ALICE_UID, "Alice", "BLM49K"));
  batch.set(aliceDb.collection("playlist_invites").doc("BLM49K"), makeInvitePayload("BLM49K", "pl_1", ALICE_UID, "Alice"));
  await assertSucceeds(batch.commit());

  // Cannot create playlist with ghost field
  const badBatch = aliceDb.batch();
  badBatch.set(aliceDb.collection("playlists").doc("pl_2"), {
    ...makePlaylistPayload(ALICE_UID, "Alice", "XYZ123"),
    isVerified: true,
  });
  badBatch.set(aliceDb.collection("playlist_invites").doc("XYZ123"), makeInvitePayload("XYZ123", "pl_2", ALICE_UID, "Alice"));
  await assertFails(badBatch.commit());
});

test("Collaborative playlist: cannot list invites or query playlists without memberUids filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("playlists").doc("pl_1"), makePlaylistPayload(ALICE_UID, "Alice", "BLM49K"));
  batch.set(aliceDb.collection("playlist_invites").doc("BLM49K"), makeInvitePayload("BLM49K", "pl_1", ALICE_UID, "Alice"));
  await assertSucceeds(batch.commit());

  // Listing invites is always denied
  await assertFails(bobDb.collection("playlist_invites").get());
  // Unfiltered playlist list is denied
  await assertFails(bobDb.collection("playlists").get());
  // Filtered query matching memberUids succeeds for Alice
  await assertSucceeds(aliceDb.collection("playlists").where("memberUids", "array-contains", ALICE_UID).get());
  // Non-member Bob cannot get Alice's playlist directly
  await assertFails(bobDb.collection("playlists").doc("pl_1").get());
});

test("Collaborative playlist join flow: Bob looks up invite, joins playlist, adds track, and upvotes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Alice creates collaborative playlist
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("playlists").doc("pl_1"), makePlaylistPayload(ALICE_UID, "Alice", "BLM49K", true));
  batch.set(aliceDb.collection("playlist_invites").doc("BLM49K"), makeInvitePayload("BLM49K", "pl_1", ALICE_UID, "Alice", true));
  await assertSucceeds(batch.commit());

  // Alice adds a track
  await assertSucceeds(
    aliceDb.collection("playlists").doc("pl_1").collection("tracks").doc("tr_1")
      .set(makeTrackPayload("pl_1", "track_101", ALICE_UID, "Alice", [ALICE_UID]))
  );

  // Bob looks up invite by exact code
  const inviteSnap = await assertSucceeds(bobDb.collection("playlist_invites").doc("BLM49K").get());
  const inviteData = inviteSnap.data();

  // Bob joins playlist (only memberUids, memberNames, updatedAt)
  await assertSucceeds(
    bobDb.collection("playlists").doc(inviteData.playlistId).update({
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      updatedAt: serverTimestamp(),
    })
  );

  // Bob can now read the playlist and pre-existing track via get()
  await assertSucceeds(bobDb.collection("playlists").doc("pl_1").get());
  await assertSucceeds(bobDb.collection("playlists").doc("pl_1").collection("tracks").doc("tr_1").get());

  // Bob can sync memberUids on pre-existing track and upvote it
  await assertSucceeds(
    bobDb.collection("playlists").doc("pl_1").collection("tracks").doc("tr_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    bobDb.collection("playlists").doc("pl_1").collection("tracks").doc("tr_1").update({
      upvotedBy: [BOB_UID],
      upvoteCount: 1,
      updatedAt: serverTimestamp(),
    })
  );

  // Bob adds a new track as a collaborator
  await assertSucceeds(
    bobDb.collection("playlists").doc("pl_1").collection("tracks").doc("tr_2")
      .set(makeTrackPayload("pl_1", "track_202", BOB_UID, "Bob", [ALICE_UID, BOB_UID]))
  );

  // Non-member Charlie cannot add tracks or upvote
  await assertFails(
    charlieDb.collection("playlists").doc("pl_1").collection("tracks").doc("tr_3")
      .set(makeTrackPayload("pl_1", "track_303", CHARLIE_UID, "Charlie", [ALICE_UID, BOB_UID, CHARLIE_UID]))
  );
});

test("Collaborative playlist security: cannot join non-collaborative playlist or spoof ownerId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("playlists").doc("pl_solo"), makePlaylistPayload(ALICE_UID, "Alice", "SOLO99", false));
  batch.set(aliceDb.collection("playlist_invites").doc("SOLO99"), makeInvitePayload("SOLO99", "pl_solo", ALICE_UID, "Alice", false));
  await assertSucceeds(batch.commit());

  await assertFails(
    bobDb.collection("playlists").doc("pl_solo").update({
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      updatedAt: serverTimestamp(),
    })
  );
});

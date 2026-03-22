import { z } from "zod";

export const clientTypes = ["android_native", "ios_web", "android_web_debug"] as const;
export type ClientType = (typeof clientTypes)[number];

export const transportModes = ["local_lan", "remote_signaling"] as const;
export type TransportMode = (typeof transportModes)[number];

export const clientRoles = ["full_voice", "console_only", "experimental_web_voice"] as const;
export type ClientRole = (typeof clientRoles)[number];

export const eventTypes = [
  "join",
  "leave",
  "speaker_start",
  "speaker_end",
  "channel_change",
  "room_created",
  "room_closed",
  "host_online",
  "host_offline",
] as const;
export type EventType = (typeof eventTypes)[number];

export const hostStatusSchema = z.enum(["online", "offline", "closed"]);
export type HostStatus = z.infer<typeof hostStatusSchema>;

export const transportModeSchema = z.enum(transportModes);
export const clientRoleSchema = z.enum(clientRoles);

export const peerCapabilitiesSchema = z.object({
  canTransmitAudio: z.boolean(),
  canReceiveAudio: z.boolean(),
  supportsLocalJoin: z.boolean(),
  supportsAdvancedWebRtc: z.boolean(),
});
export type PeerCapabilities = z.infer<typeof peerCapabilitiesSchema>;

export const roomCapabilitiesSchema = z.object({
  allowsConsoleClients: z.boolean(),
  allowsExperimentalWebVoice: z.boolean(),
  localFirst: z.boolean(),
});
export type RoomCapabilities = z.infer<typeof roomCapabilitiesSchema>;

export const hostEndpointSchema = z.object({
  hostAddress: z.string(),
  port: z.number().int().positive(),
  baseUrl: z.string().url(),
  consoleUrl: z.string().url().nullable().optional(),
});
export type HostEndpoint = z.infer<typeof hostEndpointSchema>;

export const localPairingPayloadSchema = z.object({
  roomId: z.string(),
  roomCode: z.string().min(4).max(8),
  roomName: z.string(),
  hostAddress: z.string(),
  port: z.number().int().positive(),
  protocolVersion: z.string(),
  transportMode: z.literal("local_lan"),
  roomCapabilities: roomCapabilitiesSchema,
  consoleUrl: z.string().url(),
});
export type LocalPairingPayload = z.infer<typeof localPairingPayloadSchema>;

export const createRoomRequestSchema = z.object({
  roomName: z.string().min(3).max(48),
  channelNames: z.array(z.string().min(1).max(24)).min(1).max(8),
  hostDeviceId: z.string().min(3).max(96),
  hostNickname: z.string().min(2).max(24),
});
export type CreateRoomRequest = z.infer<typeof createRoomRequestSchema>;

export const roomCodeReservationSchema = z.object({
  roomId: z.string(),
  roomCode: z.string().min(4).max(8),
  expiresAt: z.string(),
  hostSessionToken: z.string(),
  hostPeerId: z.string(),
  wsUrl: z.string().url(),
  transportMode: transportModeSchema.default("remote_signaling"),
  hostEndpoint: hostEndpointSchema.nullable().optional(),
  roomCapabilities: roomCapabilitiesSchema.optional(),
  pairingUrl: z.string().url().nullable().optional(),
});
export type RoomCodeReservation = z.infer<typeof roomCodeReservationSchema>;

export const joinRoomRequestSchema = z.object({
  roomCode: z.string().min(4).max(8),
  nickname: z.string().min(2).max(24),
  clientType: z.enum(clientTypes),
  deviceId: z.string().min(3).max(96),
  requestedRole: clientRoleSchema.optional(),
});
export type JoinRoomRequest = z.infer<typeof joinRoomRequestSchema>;

export const peerStateSchema = z.object({
  peerId: z.string(),
  nickname: z.string(),
  clientType: z.enum(clientTypes),
  deviceId: z.string(),
  selectedChannelId: z.string(),
  isHost: z.boolean(),
  role: clientRoleSchema.default("full_voice"),
  capabilities: peerCapabilitiesSchema,
  isConnected: z.boolean(),
  joinedAt: z.string(),
  lastSeenAt: z.string(),
});
export type PeerState = z.infer<typeof peerStateSchema>;

export const channelStateSchema = z.object({
  channelId: z.string(),
  name: z.string(),
  activeSpeakerPeerId: z.string().nullable(),
  queueVersion: z.number().int().nonnegative(),
});
export type ChannelState = z.infer<typeof channelStateSchema>;

export const eventEntrySchema = z.object({
  eventId: z.string(),
  roomId: z.string(),
  channelId: z.string().nullable(),
  peerId: z.string().nullable(),
  type: z.enum(eventTypes),
  occurredAt: z.string(),
  summary: z.string(),
});
export type EventEntry = z.infer<typeof eventEntrySchema>;

export const roomSnapshotSchema = z.object({
  roomId: z.string(),
  roomName: z.string(),
  roomCode: z.string(),
  channels: z.array(channelStateSchema),
  members: z.array(peerStateSchema),
  activeSpeakerByChannel: z.record(z.string(), z.string().nullable()),
  hostStatus: hostStatusSchema,
  transportMode: transportModeSchema.default("remote_signaling"),
  hostEndpoint: hostEndpointSchema.nullable().optional(),
  roomCapabilities: roomCapabilitiesSchema,
  eventLog: z.array(eventEntrySchema),
  capacity: z.number().int().positive(),
});
export type RoomSnapshot = z.infer<typeof roomSnapshotSchema>;

export const talkLockStateSchema = z.object({
  channelId: z.string(),
  holderPeerId: z.string().nullable(),
  grantedAt: z.string().nullable(),
  queueVersion: z.number().int().nonnegative(),
});
export type TalkLockState = z.infer<typeof talkLockStateSchema>;

export const signalEnvelopeSchema = z.object({
  roomId: z.string(),
  peerId: z.string(),
  targetPeerId: z.string(),
  type: z.enum(["offer", "answer", "ice-candidate"]),
  sdp: z.string().nullable().optional(),
  iceCandidate: z.string().nullable().optional(),
});
export type SignalEnvelope = z.infer<typeof signalEnvelopeSchema>;

export const joinRoomResponseSchema = z.object({
  roomId: z.string(),
  peerId: z.string(),
  peerToken: z.string(),
  wsUrl: z.string().url(),
  clientRole: clientRoleSchema.default("full_voice"),
  transportMode: transportModeSchema.default("remote_signaling"),
  hostEndpoint: hostEndpointSchema.nullable().optional(),
  snapshot: roomSnapshotSchema,
});
export type JoinRoomResponse = z.infer<typeof joinRoomResponseSchema>;

export const wsHelloMessageSchema = z.object({
  kind: z.literal("hello"),
  roomId: z.string(),
  peerId: z.string(),
  token: z.string(),
});

export const wsRoomSnapshotSchema = z.object({
  kind: z.literal("room_snapshot"),
  snapshot: roomSnapshotSchema,
});

export const wsPeerJoinedSchema = z.object({
  kind: z.literal("peer_joined"),
  peer: peerStateSchema,
});

export const wsPeerLeftSchema = z.object({
  kind: z.literal("peer_left"),
  peerId: z.string(),
});

export const wsChannelSelectSchema = z.object({
  kind: z.literal("channel_select"),
  peerId: z.string(),
  channelId: z.string(),
});

export const wsTalkRequestSchema = z.object({
  kind: z.literal("talk_request"),
  peerId: z.string(),
  channelId: z.string(),
});

export const wsTalkReleaseRequestSchema = z.object({
  kind: z.literal("talk_release_request"),
  peerId: z.string(),
  channelId: z.string(),
});

export const wsTalkGrantSchema = z.object({
  kind: z.literal("talk_granted"),
  channelId: z.string(),
  holderPeerId: z.string(),
  grantedAt: z.string(),
  queueVersion: z.number().int().nonnegative(),
});

export const wsTalkDeniedSchema = z.object({
  kind: z.literal("talk_denied"),
  channelId: z.string(),
  peerId: z.string(),
  reason: z.string(),
});

export const wsTalkReleasedSchema = z.object({
  kind: z.literal("talk_released"),
  channelId: z.string(),
  peerId: z.string(),
  queueVersion: z.number().int().nonnegative(),
});

export const wsSignalSchema = z.object({
  kind: z.literal("signal"),
  fromPeerId: z.string(),
  toPeerId: z.string(),
  signal: signalEnvelopeSchema,
});

export const wsEventSchema = z.object({
  kind: z.literal("event"),
  entry: eventEntrySchema,
});

export const wsRoomClosedSchema = z.object({
  kind: z.literal("room_closed"),
  reason: z.string(),
});

export const wsSyncSnapshotSchema = z.object({
  kind: z.literal("sync_snapshot"),
  snapshot: roomSnapshotSchema,
});

export const wsErrorSchema = z.object({
  kind: z.literal("error"),
  message: z.string(),
});

export const socketMessageSchema = z.discriminatedUnion("kind", [
  wsHelloMessageSchema,
  wsRoomSnapshotSchema,
  wsPeerJoinedSchema,
  wsPeerLeftSchema,
  wsChannelSelectSchema,
  wsTalkRequestSchema,
  wsTalkReleaseRequestSchema,
  wsTalkGrantSchema,
  wsTalkDeniedSchema,
  wsTalkReleasedSchema,
  wsSignalSchema,
  wsEventSchema,
  wsRoomClosedSchema,
  wsSyncSnapshotSchema,
  wsErrorSchema,
]);
export type SocketMessage = z.infer<typeof socketMessageSchema>;

export const DEFAULT_CHANNELS = ["Geral", "Operacao", "Suporte"] as const;
export const ROOM_CAPACITY = 10;
export const EVENT_LOG_LIMIT = 200;
export const PROTOCOL_VERSION = "2.0.0";

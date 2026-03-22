import {
  type ChannelState,
  DEFAULT_CHANNELS,
  EVENT_LOG_LIMIT,
  ROOM_CAPACITY,
  type CreateRoomRequest,
  type EventEntry,
  type JoinRoomRequest,
  type JoinRoomResponse,
  type PeerState,
  type RoomCodeReservation,
  type RoomSnapshot,
  type SignalEnvelope,
  type SocketMessage,
  type TalkLockState,
} from "@walkie/protocol";
import { randomBytes, randomUUID } from "node:crypto";

export interface SocketLike {
  send(data: string): void;
  readyState?: number;
}

const SOCKET_OPEN = 1;

interface InternalPeer extends PeerState {
  token: string;
  socket?: SocketLike;
}

interface RoomRecord {
  roomId: string;
  roomCode: string;
  roomName: string;
  expiresAt: string;
  hostSessionToken: string;
  hostPeerId: string;
  channels: ChannelState[];
  activeSpeakerByChannel: Record<string, string | null>;
  talkLocks: Record<string, TalkLockState>;
  eventLog: EventEntry[];
  hostStatus: RoomSnapshot["hostStatus"];
  peers: Map<string, InternalPeer>;
}

export interface AuthenticatedPeer {
  room: RoomRecord;
  peer: InternalPeer;
}

export class RoomRegistry {
  private readonly roomsById = new Map<string, RoomRecord>();
  private readonly roomIdByCode = new Map<string, string>();

  createRoom(input: CreateRoomRequest, wsUrl: string): RoomCodeReservation {
    const roomId = randomUUID();
    const hostPeerId = randomUUID();
    const hostSessionToken = this.makeToken();
    const roomCode = this.makeRoomCode();
    const expiresAt = new Date(Date.now() + 12 * 60 * 60 * 1000).toISOString();
    const channelNames = input.channelNames.length > 0 ? input.channelNames : [...DEFAULT_CHANNELS];
    const channels = channelNames.map<ChannelState>((name, index) => ({
      channelId: `channel-${index + 1}`,
      name,
      activeSpeakerPeerId: null,
      queueVersion: 0,
    }));
    const activeSpeakerByChannel = Object.fromEntries(channels.map((channel) => [channel.channelId, null]));
    const talkLocks = Object.fromEntries(
      channels.map((channel) => [
        channel.channelId,
        {
          channelId: channel.channelId,
          holderPeerId: null,
          grantedAt: null,
          queueVersion: 0,
        } satisfies TalkLockState,
      ]),
    );
    const now = new Date().toISOString();
    const hostPeer: InternalPeer = {
      peerId: hostPeerId,
      nickname: input.hostNickname,
      clientType: "android_native",
      deviceId: input.hostDeviceId,
      selectedChannelId: channels[0]?.channelId ?? "channel-1",
      isHost: true,
      isConnected: false,
      joinedAt: now,
      lastSeenAt: now,
      token: hostSessionToken,
    };
    const room: RoomRecord = {
      roomId,
      roomCode,
      roomName: input.roomName,
      expiresAt,
      hostSessionToken,
      hostPeerId,
      channels,
      activeSpeakerByChannel,
      talkLocks,
      eventLog: [],
      hostStatus: "offline",
      peers: new Map([[hostPeerId, hostPeer]]),
    };
    this.appendEvent(room, {
      peerId: hostPeerId,
      channelId: hostPeer.selectedChannelId,
      type: "room_created",
      summary: `${input.hostNickname} criou a sala`,
    });
    this.roomsById.set(roomId, room);
    this.roomIdByCode.set(roomCode, roomId);
    return {
      roomId,
      roomCode,
      expiresAt,
      hostSessionToken,
      hostPeerId,
      wsUrl,
    };
  }

  joinRoom(input: JoinRoomRequest, wsUrl: string): JoinRoomResponse {
    const room = this.getRoomByCodeOrThrow(input.roomCode);
    this.ensureRoomOpen(room);
    if (room.hostStatus !== "online") {
      throw new Error("A sala ainda nao esta com o host online.");
    }
    if (room.peers.size >= ROOM_CAPACITY) {
      throw new Error("A sala atingiu o limite de participantes.");
    }
    const peerId = randomUUID();
    const peerToken = this.makeToken();
    const now = new Date().toISOString();
    const peer: InternalPeer = {
      peerId,
      nickname: input.nickname,
      clientType: input.clientType,
      deviceId: input.deviceId,
      selectedChannelId: room.channels[0]?.channelId ?? "channel-1",
      isHost: false,
      isConnected: false,
      joinedAt: now,
      lastSeenAt: now,
      token: peerToken,
    };
    room.peers.set(peerId, peer);
    return {
      roomId: room.roomId,
      peerId,
      peerToken,
      wsUrl,
      snapshot: this.toSnapshot(room),
    };
  }

  authenticate(roomId: string, peerId: string, token: string): AuthenticatedPeer {
    const room = this.roomsById.get(roomId);
    if (!room) {
      throw new Error("Sala inexistente.");
    }
    const peer = room.peers.get(peerId);
    if (!peer || peer.token !== token) {
      throw new Error("Credenciais invalidas.");
    }
    return { room, peer };
  }

  attachSocket(roomId: string, peerId: string, socket: SocketLike): RoomSnapshot {
    const room = this.getRoomByIdOrThrow(roomId);
    const peer = room.peers.get(peerId);
    if (!peer) {
      throw new Error("Participante inexistente.");
    }
    peer.socket = socket;
    peer.isConnected = true;
    peer.lastSeenAt = new Date().toISOString();

    if (peer.isHost && room.hostStatus !== "online") {
      room.hostStatus = "online";
      this.appendEvent(room, {
        peerId,
        channelId: peer.selectedChannelId,
        type: "host_online",
        summary: `${peer.nickname} colocou o host online`,
      });
    }

    if (!peer.isHost) {
      this.appendEvent(room, {
        peerId,
        channelId: peer.selectedChannelId,
        type: "join",
        summary: `${peer.nickname} entrou na sala`,
      });
      this.broadcast(room, {
        kind: "peer_joined",
        peer: this.publicPeer(peer),
      });
      this.broadcastEvent(room, room.eventLog.at(-1));
    }

    const snapshot = this.toSnapshot(room);
    this.emitToPeer(peer, {
      kind: "room_snapshot",
      snapshot,
    });
    return snapshot;
  }

  detachSocket(roomId: string, peerId: string) {
    const room = this.roomsById.get(roomId);
    if (!room) {
      return;
    }
    const peer = room.peers.get(peerId);
    if (!peer) {
      return;
    }
    peer.socket = undefined;
    peer.isConnected = false;
    peer.lastSeenAt = new Date().toISOString();

    if (peer.isHost) {
      room.hostStatus = "closed";
      this.appendEvent(room, {
        peerId,
        channelId: peer.selectedChannelId,
        type: "room_closed",
        summary: `${peer.nickname} encerrou a sala`,
      });
      this.broadcast(room, {
        kind: "room_closed",
        reason: "O host Android ficou offline.",
      });
      this.roomsById.delete(roomId);
      this.roomIdByCode.delete(room.roomCode);
      return;
    }

    room.peers.delete(peerId);
    this.appendEvent(room, {
      peerId,
      channelId: peer.selectedChannelId,
      type: "leave",
      summary: `${peer.nickname} saiu da sala`,
    });
    this.broadcast(room, {
      kind: "peer_left",
      peerId,
    });
    this.broadcastEvent(room, room.eventLog.at(-1));
  }

  handleMessage(senderPeerId: string, message: SocketMessage) {
    const room = this.getRoomForPeerOrThrow(senderPeerId);
    const sender = room.peers.get(senderPeerId);
    if (!sender) {
      throw new Error("Participante desconectado.");
    }

    switch (message.kind) {
      case "channel_select": {
        sender.selectedChannelId = message.channelId;
        sender.lastSeenAt = new Date().toISOString();
        this.appendEvent(room, {
          peerId: senderPeerId,
          channelId: message.channelId,
          type: "channel_change",
          summary: `${sender.nickname} foi para ${this.channelLabel(room, message.channelId)}`,
        });
        this.broadcast(room, message);
        this.broadcastEvent(room, room.eventLog.at(-1));
        break;
      }
      case "talk_request": {
        this.emitToPeerId(room, room.hostPeerId, message);
        break;
      }
      case "talk_release_request": {
        this.emitToPeerId(room, room.hostPeerId, message);
        break;
      }
      case "talk_granted": {
        this.assertHost(room, senderPeerId);
        const channel = this.getChannelOrThrow(room, message.channelId);
        const lock = room.talkLocks[message.channelId];
        lock.holderPeerId = message.holderPeerId;
        lock.grantedAt = message.grantedAt;
        lock.queueVersion = message.queueVersion;
        channel.activeSpeakerPeerId = message.holderPeerId;
        channel.queueVersion = message.queueVersion;
        room.activeSpeakerByChannel[message.channelId] = message.holderPeerId;
        this.appendEvent(room, {
          peerId: message.holderPeerId,
          channelId: message.channelId,
          type: "speaker_start",
          summary: `${this.peerLabel(room, message.holderPeerId)} esta falando em ${channel.name}`,
        });
        this.broadcast(room, message);
        this.broadcastEvent(room, room.eventLog.at(-1));
        break;
      }
      case "talk_denied": {
        this.assertHost(room, senderPeerId);
        this.broadcast(room, message);
        break;
      }
      case "talk_released": {
        this.assertHost(room, senderPeerId);
        const channel = this.getChannelOrThrow(room, message.channelId);
        const lock = room.talkLocks[message.channelId];
        lock.holderPeerId = null;
        lock.grantedAt = null;
        lock.queueVersion = message.queueVersion;
        channel.activeSpeakerPeerId = null;
        channel.queueVersion = message.queueVersion;
        room.activeSpeakerByChannel[message.channelId] = null;
        this.appendEvent(room, {
          peerId: message.peerId,
          channelId: message.channelId,
          type: "speaker_end",
          summary: `${this.peerLabel(room, message.peerId)} liberou o canal ${channel.name}`,
        });
        this.broadcast(room, message);
        this.broadcastEvent(room, room.eventLog.at(-1));
        break;
      }
      case "signal": {
        this.emitToPeerId(room, message.toPeerId, message);
        break;
      }
      case "sync_snapshot": {
        this.assertHost(room, senderPeerId);
        room.channels = message.snapshot.channels;
        room.activeSpeakerByChannel = message.snapshot.activeSpeakerByChannel;
        room.hostStatus = message.snapshot.hostStatus;
        room.eventLog = message.snapshot.eventLog.slice(-EVENT_LOG_LIMIT);
        this.broadcast(room, {
          kind: "room_snapshot",
          snapshot: this.toSnapshot(room),
        });
        break;
      }
      case "room_closed": {
        this.assertHost(room, senderPeerId);
        this.broadcast(room, message);
        this.roomsById.delete(room.roomId);
        this.roomIdByCode.delete(room.roomCode);
        break;
      }
      case "room_snapshot":
      case "peer_joined":
      case "peer_left":
      case "event":
      case "error":
      case "hello":
        break;
    }
  }

  getSnapshotByCode(roomCode: string) {
    const room = this.getRoomByCodeOrThrow(roomCode);
    return this.toSnapshot(room);
  }

  private publicPeer(peer: InternalPeer): PeerState {
    const { token: _token, socket: _socket, ...rest } = peer;
    return rest;
  }

  private toSnapshot(room: RoomRecord): RoomSnapshot {
    return {
      roomId: room.roomId,
      roomName: room.roomName,
      roomCode: room.roomCode,
      channels: room.channels,
      members: [...room.peers.values()].map((peer) => this.publicPeer(peer)),
      activeSpeakerByChannel: room.activeSpeakerByChannel,
      hostStatus: room.hostStatus,
      eventLog: room.eventLog,
      capacity: ROOM_CAPACITY,
    };
  }

  private appendEvent(
    room: RoomRecord,
    input: Pick<EventEntry, "peerId" | "channelId" | "type" | "summary">,
  ) {
    room.eventLog = [
      ...room.eventLog,
      {
        eventId: randomUUID(),
        roomId: room.roomId,
        occurredAt: new Date().toISOString(),
        ...input,
      },
    ].slice(-EVENT_LOG_LIMIT);
  }

  private broadcast(room: RoomRecord, message: SocketMessage) {
    for (const peer of room.peers.values()) {
      this.emitToPeer(peer, message);
    }
  }

  private broadcastEvent(room: RoomRecord, event?: EventEntry) {
    if (!event) {
      return;
    }
    this.broadcast(room, {
      kind: "event",
      entry: event,
    });
  }

  private emitToPeer(peer: InternalPeer, message: SocketMessage) {
    if (peer.socket && (peer.socket.readyState === undefined || peer.socket.readyState === SOCKET_OPEN)) {
      peer.socket.send(JSON.stringify(message));
    }
  }

  private emitToPeerId(room: RoomRecord, peerId: string, message: SocketMessage) {
    const peer = room.peers.get(peerId);
    if (!peer) {
      return;
    }
    this.emitToPeer(peer, message);
  }

  private assertHost(room: RoomRecord, senderPeerId: string) {
    if (room.hostPeerId !== senderPeerId) {
      throw new Error("Apenas o host pode enviar essa mensagem.");
    }
  }

  private getRoomByCodeOrThrow(roomCode: string) {
    const roomId = this.roomIdByCode.get(roomCode.toUpperCase());
    if (!roomId) {
      throw new Error("Sala nao encontrada.");
    }
    return this.getRoomByIdOrThrow(roomId);
  }

  private getRoomByIdOrThrow(roomId: string) {
    const room = this.roomsById.get(roomId);
    if (!room) {
      throw new Error("Sala nao encontrada.");
    }
    return room;
  }

  private getRoomForPeerOrThrow(peerId: string) {
    for (const room of this.roomsById.values()) {
      if (room.peers.has(peerId)) {
        return room;
      }
    }
    throw new Error("Participante nao localizado.");
  }

  private ensureRoomOpen(room: RoomRecord) {
    if (new Date(room.expiresAt).getTime() < Date.now()) {
      this.roomsById.delete(room.roomId);
      this.roomIdByCode.delete(room.roomCode);
      throw new Error("Sala expirada.");
    }
    if (room.hostStatus === "closed") {
      throw new Error("Sala encerrada.");
    }
  }

  private getChannelOrThrow(room: RoomRecord, channelId: string) {
    const channel = room.channels.find((candidate) => candidate.channelId === channelId);
    if (!channel) {
      throw new Error("Canal invalido.");
    }
    return channel;
  }

  private channelLabel(room: RoomRecord, channelId: string) {
    return this.getChannelOrThrow(room, channelId).name;
  }

  private peerLabel(room: RoomRecord, peerId: string) {
    return room.peers.get(peerId)?.nickname ?? "Participante";
  }

  private makeRoomCode() {
    let code = "";
    do {
      code = randomBytes(3).toString("base64url").slice(0, 6).toUpperCase();
    } while (this.roomIdByCode.has(code));
    return code;
  }

  private makeToken() {
    return randomBytes(24).toString("base64url");
  }
}

import { describe, expect, it } from "vitest";
import { RoomRegistry } from "../src/roomRegistry.js";

class FakeSocket {
  readyState = 1;
  messages: string[] = [];

  send(data: string) {
    this.messages.push(data);
  }
}

describe("RoomRegistry", () => {
  it("creates a room and allows a guest to join after host is online", () => {
    const registry = new RoomRegistry();
    const reservation = registry.createRoom({
      roomName: "Equipe",
      channelNames: ["Geral", "Operacao"],
      hostDeviceId: "android-1",
      hostNickname: "Host",
    }, "ws://localhost:8787/ws");

    expect(() =>
      registry.joinRoom({
        roomCode: reservation.roomCode,
        nickname: "Ana",
        clientType: "ios_web",
        deviceId: "iphone-1",
      }, "ws://localhost:8787/ws"),
    ).toThrow("host online");

    const hostSocket = new FakeSocket();
    registry.attachSocket(reservation.roomId, reservation.hostPeerId, hostSocket);

    const joinResult = registry.joinRoom({
      roomCode: reservation.roomCode,
      nickname: "Ana",
      clientType: "ios_web",
      deviceId: "iphone-1",
    }, "ws://localhost:8787/ws");

    expect(joinResult.snapshot.roomCode).toBe(reservation.roomCode);
    expect(joinResult.snapshot.members).toHaveLength(2);
  });

  it("updates talk lock and closes room when host disconnects", () => {
    const registry = new RoomRegistry();
    const reservation = registry.createRoom({
      roomName: "Equipe",
      channelNames: ["Geral"],
      hostDeviceId: "android-1",
      hostNickname: "Host",
    }, "ws://localhost:8787/ws");
    registry.attachSocket(reservation.roomId, reservation.hostPeerId, new FakeSocket());
    const guest = registry.joinRoom({
      roomCode: reservation.roomCode,
      nickname: "Ana",
      clientType: "ios_web",
      deviceId: "iphone-1",
    }, "ws://localhost:8787/ws");
    const guestSocket = new FakeSocket();
    registry.attachSocket(guest.roomId, guest.peerId, guestSocket);

    registry.handleMessage(reservation.hostPeerId, {
      kind: "talk_granted",
      channelId: "channel-1",
      holderPeerId: guest.peerId,
      grantedAt: new Date().toISOString(),
      queueVersion: 1,
    });

    expect(
      guestSocket.messages.some((message) => message.includes('"kind":"talk_granted"')),
    ).toBe(true);

    registry.detachSocket(reservation.roomId, reservation.hostPeerId);

    expect(
      guestSocket.messages.some((message) => message.includes('"kind":"room_closed"')),
    ).toBe(true);
  });
});

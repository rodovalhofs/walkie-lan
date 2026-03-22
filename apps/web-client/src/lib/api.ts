import {
  type CreateRoomRequest,
  createRoomRequestSchema,
  type JoinRoomRequest,
  type JoinRoomResponse,
  joinRoomRequestSchema,
  joinRoomResponseSchema,
  type RoomCodeReservation,
  roomCodeReservationSchema,
} from "@walkie/protocol";

export async function createRoom(baseUrl: string, payload: CreateRoomRequest) {
  createRoomRequestSchema.parse(payload);
  const response = await fetch(`${normalizeBaseUrl(baseUrl)}/api/rooms`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error ?? "Falha ao criar sala.");
  }
  return roomCodeReservationSchema.parse(data) satisfies RoomCodeReservation;
}

export async function joinRoom(baseUrl: string, payload: JoinRoomRequest) {
  joinRoomRequestSchema.parse(payload);
  const response = await fetch(`${normalizeBaseUrl(baseUrl)}/api/rooms/join`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error ?? "Falha ao entrar na sala.");
  }
  return joinRoomResponseSchema.parse(data) satisfies JoinRoomResponse;
}

export function normalizeBaseUrl(baseUrl: string) {
  return baseUrl.replace(/\/+$/, "");
}


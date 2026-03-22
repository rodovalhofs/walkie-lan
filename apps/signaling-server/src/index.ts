import type { Request } from "express";
import cors from "cors";
import express from "express";
import { createServer } from "node:http";
import { WebSocketServer } from "ws";
import {
  createRoomRequestSchema,
  joinRoomRequestSchema,
  socketMessageSchema,
} from "@walkie/protocol";
import { getServerConfig } from "./config.js";
import { RoomRegistry } from "./roomRegistry.js";

const config = getServerConfig();
const registry = new RoomRegistry();
const app = express();
app.use(cors());
app.use(express.json());

app.get("/health", (_request, response) => {
  response.json({ ok: true });
});

app.post("/api/rooms", (request, response) => {
  const parseResult = createRoomRequestSchema.safeParse(request.body);
  if (!parseResult.success) {
    response.status(400).json({ error: parseResult.error.flatten() });
    return;
  }

  try {
    const reservation = registry.createRoom(parseResult.data, resolveWsUrl(request));
    response.status(201).json(reservation);
  } catch (error) {
    response.status(400).json({ error: getErrorMessage(error) });
  }
});

app.post("/api/rooms/join", (request, response) => {
  const parseResult = joinRoomRequestSchema.safeParse(request.body);
  if (!parseResult.success) {
    response.status(400).json({ error: parseResult.error.flatten() });
    return;
  }

  try {
    const joinResponse = registry.joinRoom(parseResult.data, resolveWsUrl(request));
    response.json(joinResponse);
  } catch (error) {
    response.status(400).json({ error: getErrorMessage(error) });
  }
});

app.get("/api/rooms/:roomCode", (request, response) => {
  try {
    const snapshot = registry.getSnapshotByCode(request.params.roomCode);
    response.json(snapshot);
  } catch (error) {
    response.status(404).json({ error: getErrorMessage(error) });
  }
});

const httpServer = createServer(app);
const wss = new WebSocketServer({ server: httpServer, path: "/ws" });

wss.on("connection", (socket) => {
  let connection: { roomId: string; peerId: string } | null = null;

  socket.on("message", (payload) => {
    try {
      const raw = JSON.parse(payload.toString());
      const message = socketMessageSchema.parse(raw);

      if (!connection) {
        if (message.kind !== "hello") {
          socket.send(JSON.stringify({ kind: "error", message: "Mensagem hello obrigatoria." }));
          socket.close();
          return;
        }
        const auth = registry.authenticate(message.roomId, message.peerId, message.token);
        connection = { roomId: auth.room.roomId, peerId: auth.peer.peerId };
        registry.attachSocket(auth.room.roomId, auth.peer.peerId, socket);
        return;
      }

      registry.handleMessage(connection.peerId, message);
    } catch (error) {
      socket.send(JSON.stringify({ kind: "error", message: getErrorMessage(error) }));
    }
  });

  socket.on("close", () => {
    if (!connection) {
      return;
    }
    registry.detachSocket(connection.roomId, connection.peerId);
  });
});

httpServer.listen(config.port, () => {
  console.log(`Walkie signaling server on ${config.publicHttpBaseUrl ?? `http://0.0.0.0:${config.port}`}`);
});

function resolveWsUrl(request: Request) {
  if (config.publicWsBaseUrl) {
    return config.publicWsBaseUrl;
  }

  const forwardedProto = request.header("x-forwarded-proto");
  const requestProtocol = forwardedProto ?? request.protocol;
  const wsProtocol = requestProtocol === "https" ? "wss" : "ws";
  return `${wsProtocol}://${request.get("host")}/ws`;
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "Erro inesperado.";
}

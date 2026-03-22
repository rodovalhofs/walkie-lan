import { type SocketMessage, socketMessageSchema } from "@walkie/protocol";

interface SignalingOptions {
  roomId: string;
  peerId: string;
  token: string;
  wsUrl: string;
  onMessage: (message: SocketMessage) => void;
  onClose?: () => void;
  onError?: (error: Error) => void;
}

export class SignalingClient {
  private socket: WebSocket | null = null;

  connect(options: SignalingOptions) {
    this.socket = new WebSocket(options.wsUrl);
    this.socket.addEventListener("open", () => {
      this.send({
        kind: "hello",
        roomId: options.roomId,
        peerId: options.peerId,
        token: options.token,
      });
    });
    this.socket.addEventListener("message", (event) => {
      try {
        const parsed = socketMessageSchema.parse(JSON.parse(event.data));
        options.onMessage(parsed);
      } catch (error) {
        options.onError?.(
          error instanceof Error ? error : new Error("Mensagem de sinalizacao invalida."),
        );
      }
    });
    this.socket.addEventListener("error", () => {
      options.onError?.(new Error("Falha no canal de sinalizacao."));
    });
    this.socket.addEventListener("close", () => {
      options.onClose?.();
    });
  }

  send(message: SocketMessage) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return;
    }
    this.socket.send(JSON.stringify(message));
  }

  disconnect() {
    this.socket?.close();
    this.socket = null;
  }
}

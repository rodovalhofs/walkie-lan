declare module "simple-peer/simplepeer.min.js" {
  import Peer from "simple-peer";
  export default Peer;
}

interface MediaDevices {
  selectAudioOutput?: (options?: { deviceId?: string }) => Promise<MediaDeviceInfo>;
}

interface HTMLMediaElement {
  setSinkId?(sinkId: string): Promise<void>;
  sinkId?: string;
}

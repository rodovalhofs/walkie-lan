export function getServerConfig() {
  const port = Number(process.env.PORT ?? 8787);
  const publicHttpBaseUrl = process.env.PUBLIC_HTTP_BASE_URL ?? null;
  const publicWsBaseUrl = process.env.PUBLIC_WS_BASE_URL ?? null;

  return {
    port,
    publicHttpBaseUrl,
    publicWsBaseUrl,
  };
}

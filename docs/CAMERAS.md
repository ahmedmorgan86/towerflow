# Camera integration

Camera management is available in Arabic/English web and native Android. Managers add devices per active building and grant named organization members viewing access, optionally with expiry. Device credentials are encrypted using AES-256-GCM and never returned to clients. Each video request checks organization membership, camera access, building status and session ownership. Sessions last five minutes, with up to four per user. Changing configuration or grants revokes existing application access.

## Deployment prerequisites

Apply database migration `010_cameras.sql` through the normal reviewed migration process before deploying this branch. Set `CAMERA_ENCRYPTION_KEY` to 32 cryptographically random bytes encoded as 64 hex characters in server secrets. Back it up securely: losing or replacing it without re-encryption prevents reading existing camera configurations. Never put it in Git or the Android package.

Install a dedicated gateway in each building network, Node 24 and FFmpeg/ffprobe with H.264 encoding support. In `gateway`, run `npm ci`, copy `.env.example` to private `.env.local`, and set a unique random token of at least 32 characters. Explicitly allow only device subnets and necessary ports. No subnet is allowed by default. Camera addresses must be literal private IPv4 addresses; DNS, public IP, loopback, link-local and IPv6 addresses are rejected.

Run `npm start`. The gateway binds to 127.0.0.1:8090. Publish it through a properly configured HTTPS reverse proxy or private tunnel accessible to the application server. Keep bearer authorization enabled; do not expose camera ports to the public internet. Configure `CAMERA_GATEWAYS` on the application server as a JSON object mapping building UUIDs to `{ "origin": "https://your-gateway.example", "token": "your-private-token" }`. Gateway credentials must never be exposed to clients.

Restrict the gateway operating-system firewall and service account to the explicitly approved camera IPs/ports. Network restrictions are necessary because camera/RTSP servers may redirect requests. Run gateway processes under a dedicated account, prevent other users from inspecting process arguments, and use a private encrypted temporary volume. FFmpeg receives camera credentials in its process arguments. Video segments are temporary; process crashes can leave temporary files requiring operator cleanup. Closing a viewer or revoking access stops application access immediately; upstream processing expires within five minutes. Maximum gateway streams defaults to eight; probes/start operations are serialized. Set capacity according to actual CPU/network measurements.

## Supported integration paths

* RTSP: supply the stream URL without embedded credentials; username/password have separate fields.
* ONVIF: supply the device service HTTP(S) endpoint. Connection tests list profile tokens; select a token to view a DVR/NVR channel. Returned stream addresses must use the same device IP.
* Vendor: an operator-installed reviewed adapter is required. See `gateway/adapters/README.md`. No proprietary brand adapters ship enabled.

Universal brand/model compatibility is not promised. Devices need an accessible compatible stream and appropriate account permissions. Cloud-only/P2P cameras may require a licensed vendor SDK. PTZ, recordings playback and audio are not implemented or advertised as enabled. This feature provides live viewing, not a video archive.

Use a dedicated camera account with minimum viewing permissions. Grant residents only cameras suitable for their access; building membership alone does not grant camera viewing. A manager can disable a camera or revoke a member immediately.

## Verification and limits

Run root `npm test`, `npm run build`, gateway `npm test`, and Android `assembleDebug lint`. API tests use real local PostgreSQL-compatible PGlite and HTTP, but gateway responses are simulated for authorization tests. Gateway tests verify network boundaries and authentication. Physical cameras, ONVIF interoperability, real FFmpeg video and mobile playback still require installation acceptance testing on the building network. No cloud migration or production activation is implied by a successful local test.

Acceptance on installation: test actual device profiles, play web and Android video, verify a resident cannot view an ungranted camera, revoke access during playback, test disabled/archived buildings, check firewall restrictions, and measure latency/load with the intended number of viewers. Android requires its own APK/AAB build and signing; Vercel hosts the web/API only.

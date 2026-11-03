# Vendor adapters

ONVIF and manual RTSP are built in. A brand name alone never implies support.
Install a reviewed JavaScript module in this folder, then explicitly allow it in CAMERA_VENDOR_ADAPTERS, for example {"vendor_name":"./adapters/vendor-name.mjs"}.
The module must export async resolve(config) returning {url: "rtsp://PRIVATE_IPV4/...", profiles: [{token,name}]}. Credentials are passed in config; never log them. The resulting URL must point to the same allowlisted device IP, on an allowed RTSP port, without embedded credentials. Core code supplies the stored username/password to FFmpeg.

Use only the vendor's authorized SDK/API and license. Adapters are trusted operator-installed code; device users cannot supply module paths. API adapters needing cloud hosts, audio, PTZ or recordings require a separate reviewed network/access contract; they are not enabled by this live-video adapter interface.

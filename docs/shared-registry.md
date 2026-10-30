# Shared registry design (next implementation step)

The current client deliberately does not transmit or fetch player tags. To support the requested hybrid mode, deploy a small HTTPS registry and then add an opt-in client integration.

## Suggested API

- `GET /v1/tags/{normalizedPlayerName}` → `{ "text": "they/them", "updatedAt": "..."}`
- `PUT /v1/tags/{normalizedPlayerName}` with `{ "text": "custom text" }` and a per-user publish token
- `DELETE /v1/tags/{normalizedPlayerName}` to unpublish
- `GET /v1/health` for availability checks

## Behaviour

1. Publishing is off by default and requires an explicit opt-in in plugin settings.
2. The local player may publish only their own tag.
3. A published tag is a fallback for other players; a local override takes precedence.
4. A local hidden-player entry suppresses both published tags and local overrides.
5. Cache lookups and refresh them periodically; do not make a web request every render frame.
6. Provide an unpublish control and a clear explanation of what is shared.
7. Validate and cap text length server-side as well as client-side; reject control characters and markup.

## Precedence

`hidden player` → no tag  
`local override` → show local override  
`published registry entry` → show published text  
otherwise → no tag

## Hosting note

The registry needs a public HTTPS endpoint and abuse controls (rate limiting, moderation/reporting, deletion). The plugin should not ship with an untrusted or undocumented endpoint hard-coded into it.

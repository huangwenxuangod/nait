# Nail-It Supabase Scaffold

This directory is the first implementation pass of the formal backend from
`StyleMirror_Backend_API_PRD.md`.

## Included

- `config.toml`
- Initial SQL migration
- Shared function helpers and request/response types
- 6 Edge Function skeletons:
  - `create_session`
  - `submit_source_link`
  - `prepare_asset_upload`
  - `confirm_asset_upload`
  - `create_try_on`
  - `generate_execution_package`

## Current Reality

These functions already:

- accept the expected request shapes,
- write to the intended tables,
- move session state forward,
- return stable JSON responses.

They do **not** yet perform the real AI work:

- source link resolution is still placeholder,
- source parsing is still placeholder,
- try-on generation is still placeholder,
- BOM/SOP generation is still placeholder.

## Next Backend Steps

1. Replace placeholder source parsing in `submit_source_link`.
2. Add real upload signing in `prepare_asset_upload`.
3. Add real try-on model call in `create_try_on`.
4. Add real BOM + SOP generation in `generate_execution_package`.
5. Wire Android repositories to uploads, Realtime, and status polling.

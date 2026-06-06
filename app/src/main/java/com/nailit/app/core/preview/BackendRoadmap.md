# Nail-It Backend Implementation Notes

This file is a local engineering note that mirrors the formal backend PRD.

Current status:

1. `supabase/config.toml` exists.
2. Initial SQL migration exists.
3. Six Edge Function skeletons exist.
4. Android-side request/response contracts exist.

Still missing before the backend is truly functional:

1. Real source link resolution and video ingestion.
2. Real style parsing model integration.
3. Real try-on generation pipeline.
4. Real BOM and SOP generation logic.
5. Android repository layer that calls the Supabase functions.
6. Realtime subscription wiring in the app.

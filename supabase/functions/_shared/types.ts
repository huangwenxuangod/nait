export type SessionStatus =
  | "draft"
  | "source_submitted"
  | "source_parsing"
  | "source_parsed"
  | "hand_uploaded"
  | "try_on_pending"
  | "try_on_ready"
  | "bom_ready"
  | "sop_ready"
  | "in_progress"
  | "completed"
  | "failed";

export type AssetType =
  | "tutorial_frame"
  | "hand_photo"
  | "try_on_result"
  | "sop_media";

export interface CreateSessionRequest {
  install_id: string;
  source_type: string;
}

export interface CreateSessionResponse {
  session_id: string;
  status: SessionStatus;
}

export interface SubmitSourceLinkRequest {
  session_id: string;
  source_url: string;
}

export interface SubmitSourceLinkResponse {
  session_id: string;
  status: SessionStatus;
}

export interface PrepareAssetUploadRequest {
  session_id: string;
  asset_type: AssetType;
  mime_type: string;
}

export interface PrepareAssetUploadResponse {
  asset_id: string;
  storage_path: string;
  bucket: string;
}

export interface ConfirmAssetUploadRequest {
  session_id: string;
  asset_id: string;
  asset_type: AssetType;
  storage_path: string;
}

export interface ConfirmAssetUploadResponse {
  ok: true;
}

export interface CreateTryOnRequest {
  session_id: string;
}

export interface CreateTryOnResponse {
  session_id: string;
  status: SessionStatus;
}

export interface GenerateExecutionPackageRequest {
  session_id: string;
}

export interface GenerateExecutionPackageResponse {
  session_id: string;
  status: SessionStatus;
}

export interface CreateRealtimeTokenResponse {
  token: string;
  expires_at?: string | null;
  websocket_url: string;
  model: string;
}

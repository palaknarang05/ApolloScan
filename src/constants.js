// Configure via .env.local for local dev — never commit real tokens.
export const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || "http://localhost:5555";
export const REPO_URL    = import.meta.env.VITE_REPO_URL    || "";
export const TOKEN       = import.meta.env.VITE_GITHUB_TOKEN || "";

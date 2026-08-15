# ApolloScan — AI Compatibility Analyzer

ApolloScan analyzes GitHub Java repositories and scores them for **AI code tool compatibility** (GitHub Copilot, Cursor, etc.). It detects patterns that hinder AI adoption: high cyclomatic complexity, tight coupling, dynamic code constructs, and poor comment quality.

---

## Architecture

```
┌─────────────────────┐         ┌──────────────────────────────────┐
│   React Frontend    │  HTTP   │     Spring Boot Backend (5555)   │
│   (Vite + MUI)      │ ──────► │  LangChain4j + Ollama (llama3)   │
│                     │         │  GitHub API  │  Checkstyle        │
└─────────────────────┘         └──────────────────────────────────┘
```

| Layer | Tech |
|-------|------|
| Frontend | React 18, Vite, Material UI v7, MUI X Charts/DataGrid |
| Backend | Spring Boot 3.5, LangChain4j, Ollama (llama3) |
| AI | Local LLM via Ollama — no external API keys required |
| Code Analysis | Maven Checkstyle Plugin, custom metrics |

---

## Prerequisites

- **Node.js** 18+
- **Java** 21+
- **Maven** 3.9+
- **Ollama** running locally with `llama3` pulled: `ollama pull llama3`

---

## Quick Start

### 1. Backend
```bash
# Set your GitHub base URL in src/main/resources/application.properties if needed
./mvnw spring-boot:run
# Backend starts on http://localhost:5555
```

### 2. Frontend
```bash
# Install dependencies
npm install

# Copy env template and fill in values
cp .env.example .env.local

# Start dev server
npm run dev
# Opens http://localhost:5173
```

### 3. Scan a Repository
1. Click **"Scan Repository"** in the sidebar or header
2. Enter a GitHub repo URL (e.g. `https://github.com/org/repo`)
3. Enter a GitHub personal access token with `repo:read` scope
4. Click **Start Scan** — results appear on the Dashboard when complete

---

## Features

| Feature | Description |
|---------|-------------|
| **Dashboard** | Overview cards: AI Compatibility Score, Cyclomatic Complexity, Checkstyle Issues, Incompatible File Count |
| **Project Summary** | AI-generated executive summary of the repository from `pom.xml` analysis |
| **Language Breakdown** | Pie chart of language distribution and lines of code |
| **Issues** | Per-file issue list with severity, confidence, suggested fixes, and one-click GitHub issue creation |
| **Positives** | Per-file code patterns that are already AI-tool-friendly |
| **Reports** | Aggregate charts and full scan data grid |
| **HTML Export** | `/scan-html-report` endpoint generates a standalone dark-themed HTML report |

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/all-scanned-repo` | List all previously scanned repos |
| `PUT` | `/scan-repo` | Scan a repo (metadata + AI summary) |
| `PUT` | `/scan-documents-with-templates` | Deep per-file AI analysis |
| `PUT` | `/get-reports` | Get scan results for a repo |
| `PUT` | `/get-codes` | Get source lines for a specific issue |
| `PUT` | `/get-positives` | Get source lines for a specific positive |
| `PUT` | `/create-issues` | Push issues to GitHub |
| `PUT` | `/scan-html-report` | Generate downloadable HTML report |
| `POST` | `/get-checkstyle-result` | Run Checkstyle on a repo |

---

## Configuration

`src/main/resources/application.properties`:

```properties
server.port=5555
ollama.chat-model.endpoint=http://localhost:11434
ollama.chat-model.model=llama3
github.server.base.url=https://github.com
```

Frontend `.env.local`:

```env
VITE_BACKEND_URL=http://localhost:5555
VITE_REPO_URL=https://github.com/your-org/your-repo   # optional
VITE_GITHUB_TOKEN=ghp_...                              # optional, dev only
```

---

## Project Structure

```
ai-compatibility/
├── src/
│   ├── main/java/com/example/aiproject/
│   │   ├── ai/               # LangChain4j AI services + Ollama init
│   │   ├── controller/       # REST endpoints
│   │   ├── core/             # GitHub API client
│   │   ├── model/            # ScanMetrics, ScanResult, etc.
│   │   └── service/          # CodeScanService, CheckstyleService
│   └── (React frontend)
│       ├── components/       # Dashboard, Issues, Positives, Reports, charts
│       ├── shared-theme/     # MUI theme + dark/light mode
│       └── theme/customizations/
├── .env.example
├── package.json
└── pom.xml
```

---

## Development Notes

- The backend keeps scanned results **in-memory** (`ConcurrentHashMap`). Restart clears history.  
  For persistence, wire up a database to `CodeScanService.lastScannedRepos`.
- AI scoring uses a deep scan per-file. For large repos, allow several minutes.
- A `reports.json` seed file can be placed in `src/main/resources/` to serve as fallback data when no live scan exists.


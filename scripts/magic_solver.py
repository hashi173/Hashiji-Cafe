"""
Magic SQL Solver - Auto-solve SQL exercises using OpenRouter API.

Watches magic.sql for changes, extracts unsolved blocks,
calls OpenRouter API (inclusionai/ring-2.6-1t:free), writes solutions back.

Usage:
    python scripts/magic_solver.py

Input format in magic.sql (2 lines per block, separated by ----):
    yêu cầu bài toán
    SELECT * FROM test_query;
    ----

Output format (3 lines per block):
    yêu cầu bài toán
    CREATE OR REPLACE FUNCTION/TRIGGER ...
    SELECT * FROM test_query;
    ----
"""

import os
import re
import sys
import time
import json
import pathlib
import requests
from datetime import datetime
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from dotenv import load_dotenv

load_dotenv()


def log(msg: str, level: str = "INFO"):
    ts = datetime.now().strftime("%H:%M:%S")
    print(f"[{ts}] [{level}] {msg}")


def log_block(title: str, content: str, max_lines: int = 20):
    ts = datetime.now().strftime("%H:%M:%S")
    print(f"[{ts}] ┌─ {title}")
    for line in content.splitlines()[:max_lines]:
        print(f"[{ts}] │ {line}")
    if content.count("\n") >= max_lines:
        print(f"[{ts}] │ ... ({content.count(chr(10))} lines total)")
    print(f"[{ts}] └─")

OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY")
MODEL = "inclusionai/ring-2.6-1t:free"
API_URL = "https://openrouter.ai/api/v1/chat/completions"

PROJECT_ROOT = pathlib.Path(__file__).resolve().parent.parent
MAGIC_SQL = PROJECT_ROOT / "magic.sql"
SCHEMA_FILE = PROJECT_ROOT / "docs" / "hashiji-schema.dbml"
CONTEXT_FILE = PROJECT_ROOT / "query" / "context.md"

DEBOUNCE_SECONDS = 1.0

SCHEMA_CACHE = ""
CONTEXT_CACHE = ""

SYSTEM_PROMPT = """You are a PostgreSQL experienced student. Write a solution for the given requirement.

SCHEMA (Hashiji-Cafe, 15 tables):
{schema}

CONTEXT:
{context}

CRITICAL RULES:
- NO COMMENTS in code. No -- lines. Pure SQL only.
- DO NOT modify or rewrite the original requirement.
- Return ONLY the SQL code. No markdown fences. No explanation.
- If you add any comment (--), the answer is WRONG."""


def load_schema() -> str:
    global SCHEMA_CACHE
    if SCHEMA_CACHE:
        return SCHEMA_CACHE
    if SCHEMA_FILE.exists():
        SCHEMA_CACHE = SCHEMA_FILE.read_text(encoding="utf-8")
    return SCHEMA_CACHE


def load_context() -> str:
    global CONTEXT_CACHE
    if CONTEXT_CACHE:
        return CONTEXT_CACHE
    if CONTEXT_FILE.exists():
        CONTEXT_CACHE = CONTEXT_FILE.read_text(encoding="utf-8")
    return CONTEXT_CACHE


def parse_blocks(content: str) -> list[dict]:
    """Parse magic.sql into blocks. Each block separated by ----"""
    raw_blocks = content.split("----")
    blocks = []
    for raw in raw_blocks:
        text = raw.strip()
        if not text:
            continue
        lines = [l for l in text.splitlines() if l.strip()]

        # A block is "solved" if it contains any SQL code line
        has_code = any(is_code_line(strip_comment(l)) for l in lines)

        block = {
            "raw": text,
            "lines": lines,
            "solved": has_code,
        }
        blocks.append(block)

    log(f"Parsed {len(blocks)} block(s) from magic.sql")
    for i, b in enumerate(blocks):
        status = "SOLVED" if b["solved"] else "UNSOLVED"
        preview = b["lines"][0][:60] if b["lines"] else "(empty)"
        log(f"  Block {i}: [{status}] {preview}...")

    return blocks


def strip_comment(line: str) -> str:
    """Strip SQL comment prefix (-- ) from a line."""
    s = line.strip()
    if s.startswith("--"):
        s = s[2:].strip()
    return s


def is_code_line(line: str) -> bool:
    """Check if a line looks like SQL code (not a Vietnamese requirement)."""
    code_keywords = [
        "CREATE", "BEGIN", "SELECT", "INSERT", "UPDATE", "DELETE",
        "DO $$", "DROP", "ALTER", "WITH", "RETURN", "EXECUTE",
    ]
    upper = line.strip().upper()
    return any(upper.startswith(kw) for kw in code_keywords)


def extract_unsolved(blocks: list[dict]) -> list[tuple[int, str, list[str]]]:
    """Return list of (block_index, requirement_text, original_comment_lines) for unsolved blocks."""
    unsolved = []
    for i, block in enumerate(blocks):
        if block["solved"]:
            continue
        if not block["lines"]:
            continue

        # Collect all non-code lines as requirement (strip -- prefixes)
        req_lines = []
        comment_lines = []
        for line in block["lines"]:
            stripped = strip_comment(line)
            if not stripped:
                continue
            if is_code_line(stripped):
                continue
            req_lines.append(stripped)
            comment_lines.append(line)

        if not req_lines:
            continue

        requirement = " ".join(req_lines)
        unsolved.append((i, requirement, comment_lines))
    return unsolved


def call_openrouter(requirement: str) -> str | None:
    """Call OpenRouter API to solve the SQL requirement."""
    schema = load_schema()
    context = load_context()
    system_msg = SYSTEM_PROMPT.format(schema=schema, context=context)

    headers = {
        "Authorization": f"Bearer {OPENROUTER_API_KEY}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://hashiji.cafe",
    }

    payload = {
        "model": MODEL,
        "messages": [
            {"role": "system", "content": system_msg},
            {"role": "user", "content": requirement},
        ],
        "temperature": 0.1,
        "max_tokens": 2048,
    }

    log(f"Model: {MODEL}")
    log(f"Prompt length: {len(system_msg)} chars (system) + {len(requirement)} chars (user)")
    log(f"Requirement: {requirement[:100]}...")

    for attempt in range(3):
        try:
            log(f"API request attempt {attempt + 1}/3...")
            t0 = time.time()
            resp = requests.post(API_URL, headers=headers, json=payload, timeout=60)
            elapsed = time.time() - t0

            log(f"Response: {resp.status_code} in {elapsed:.1f}s")

            if resp.status_code == 429:
                wait = 15 * (attempt + 1)
                log(f"Rate limited (429). Waiting {wait}s...", "WARN")
                time.sleep(wait)
                continue

            if resp.status_code >= 400:
                log_block("Error response body", resp.text[:500])
                resp.raise_for_status()

            data = resp.json()

            # Log usage stats if available
            usage = data.get("usage", {})
            if usage:
                log(f"Tokens: prompt={usage.get('prompt_tokens', '?')}, completion={usage.get('completion_tokens', '?')}, total={usage.get('total_tokens', '?')}")

            # Log model info if available
            resp_model = data.get("model", MODEL)
            log(f"Response model: {resp_model}")

            # Log finish reason
            choices = data.get("choices", [])
            if choices:
                finish = choices[0].get("finish_reason", "unknown")
                log(f"Finish reason: {finish}")

            raw_content = choices[0]["message"]["content"]
            log_block("Raw model response", raw_content)

            cleaned = clean_response(raw_content)
            log_block("Cleaned SQL", cleaned)

            return cleaned

        except requests.exceptions.HTTPError as e:
            log(f"HTTP error (attempt {attempt + 1}): {e}", "ERROR")
            if resp.status_code >= 500 and attempt < 2:
                log("Server error, retrying in 5s...", "WARN")
                time.sleep(5)
                continue
            return None
        except requests.exceptions.Timeout:
            log(f"Timeout (attempt {attempt + 1}/3)", "ERROR")
            if attempt < 2:
                time.sleep(3)
                continue
            return None
        except Exception as e:
            log(f"Error (attempt {attempt + 1}): {type(e).__name__}: {e}", "ERROR")
            if attempt < 2:
                time.sleep(5)

    return None


def clean_response(text: str) -> str:
    """Remove markdown fences and extra whitespace from API response."""
    text = re.sub(r"```sql\s*", "", text)
    text = re.sub(r"```\s*", "", text)
    text = text.strip()
    return text


def solve_blocks(blocks: list[dict], unsolved: list[tuple[int, str, list[str]]]) -> list[dict]:
    """Solve unsolved blocks and update the block list."""
    log(f"Solving {len(unsolved)} block(s)...")
    for idx, requirement, comment_lines in unsolved:
        log(f"─── Block {idx} ───")
        solution = call_openrouter(requirement)
        if solution:
            blocks[idx]["lines"] = comment_lines + [solution]
            blocks[idx]["solved"] = True
            log(f"Block {idx} solved OK ({len(solution)} chars)", "OK")
        else:
            log(f"Block {idx} FAILED", "ERROR")
    return blocks


def write_blocks(blocks: list[dict]) -> None:
    """Write all blocks back to magic.sql."""
    parts = []
    for block in blocks:
        parts.append("\n".join(block["lines"]))
    content = "\n----\n".join(parts)
    if parts:
        content += "\n----\n"
    MAGIC_SQL.write_text(content, encoding="utf-8")
    log(f"Written {len(content)} chars to magic.sql ({len(blocks)} blocks)")


def process_magic_sql():
    """Main processing: read, parse, solve, write."""
    if not MAGIC_SQL.exists():
        log("magic.sql not found, creating empty file...", "WARN")
        MAGIC_SQL.write_text("", encoding="utf-8")
        return

    content = MAGIC_SQL.read_text(encoding="utf-8")
    if not content.strip():
        log("magic.sql is empty, skipping")
        return

    log(f"Read magic.sql ({len(content)} chars, {content.count(chr(10))} lines)")
    blocks = parse_blocks(content)
    unsolved = extract_unsolved(blocks)

    if not unsolved:
        log("No unsolved blocks found")
        return

    log(f"Found {len(unsolved)} unsolved block(s)")
    blocks = solve_blocks(blocks, unsolved)
    write_blocks(blocks)
    log("Done!", "OK")


class MagicSQLHandler(FileSystemEventHandler):
    def __init__(self):
        self._last_trigger = 0

    def on_modified(self, event):
        if event.is_directory:
            return
        if not event.src_path.endswith("magic.sql"):
            return

        now = time.time()
        if now - self._last_trigger < DEBOUNCE_SECONDS:
            return
        self._last_trigger = now

        log("File change detected: magic.sql")
        process_magic_sql()


def main():
    if not OPENROUTER_API_KEY:
        log("OPENROUTER_API_KEY not set!", "ERROR")
        log("Create scripts/.env with: OPENROUTER_API_KEY=your_key", "ERROR")
        sys.exit(1)

    log(f"API Key: {OPENROUTER_API_KEY[:8]}...{OPENROUTER_API_KEY[-4:]}")
    log(f"Model: {MODEL}")
    log(f"Schema: {SCHEMA_FILE} ({'found' if SCHEMA_FILE.exists() else 'MISSING'})")
    log(f"Context: {CONTEXT_FILE} ({'found' if CONTEXT_FILE.exists() else 'MISSING'})")
    log(f"Target: {MAGIC_SQL}")
    log("Watching for changes... (Ctrl+C to stop)")
    print()

    process_magic_sql()

    handler = MagicSQLHandler()
    observer = Observer()
    observer.schedule(handler, str(PROJECT_ROOT), recursive=False)
    observer.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        log("Stopping...", "WARN")
        observer.stop()
    observer.join()
    log("Stopped.")


if __name__ == "__main__":
    main()

# aTerminal Architecture

## 1. Architecture Summary

aTerminal uses Android as a secure SSH client and interaction layer. The remote machine remains the execution environment. tmux is the process persistence layer. Codex and Claude Code are launched through shell command templates inside tmux sessions. The Android app keeps a faithful terminal view while offering agent-aware session management and a best-effort reading mode over captured terminal output.

MVP deliberately avoids a required remote daemon. This keeps setup simple and preserves the user's existing SSH security model.

```text
Android App
  |
  | SSH transport
  v
Remote Host
  |
  | tmux sessions
  v
Codex / Claude Code / shell
```

## 2. Major Components

### 2.1 Android Client

Responsibilities:

- Store SSH hosts, workspaces, and app preferences.
- Manage SSH connections.
- Open interactive PTY channels.
- Run non-interactive control commands over separate SSH exec channels.
- Render terminal output.
- Render agent reading mode.
- Send quick actions and prompts to the remote PTY.
- Persist local metadata for hosts, workspaces, and recently launched agent sessions.

Recommended stack:

- Kotlin.
- Jetpack Compose.
- Room for local metadata.
- DataStore for simple preferences.
- Android Keystore-backed encryption for sensitive values.
- SSHJ or Apache Mina SSHD for SSH.

### 2.2 SSH Transport

The SSH layer supports two channel types:

- Interactive PTY channel for the visible terminal.
- Exec channel for control commands such as `tmux list-sessions`, `tmux capture-pane`, and remote capability detection.

Required SSH behavior:

- Verify and pin host fingerprints.
- Support private-key authentication.
- Support password authentication only if explicitly enabled.
- Keepalive configured per host.
- Reconnect flow that can reattach to the last tmux session.
- Do not log command payloads that may contain secrets.

### 2.3 Terminal Renderer

Responsibilities:

- Parse VT/ANSI escape sequences.
- Maintain terminal screen buffer.
- Maintain scrollback buffer.
- Support resize.
- Support text selection and copy.
- Support paste with multiline confirmation.
- Provide raw terminal mode as the source of truth.

Important Codex setting:

```bash
codex --no-alt-screen
```

For Codex, `--no-alt-screen` should be the default because it preserves terminal scrollback better inside tmux and makes reading mode extraction more reliable.

### 2.4 tmux Controller

The tmux controller owns process persistence and session lifecycle.

Control commands:

```bash
tmux list-sessions -F "#{session_name}\t#{session_created}\t#{session_attached}\t#{session_windows}"
tmux has-session -t "<session>"
tmux new-session -As "<session>" -c "<cwd>" "<command>"
tmux attach-session -t "<session>"
tmux detach-client
tmux kill-session -t "<session>"
tmux capture-pane -t "<session>" -p -J -S -3000
```

The Android app must quote session names, cwd paths, and commands before sending them to the remote shell.

Session naming convention:

```text
aterm:<agent>:<workspace-slug>
aterm:<agent>:<workspace-slug>:<short-id>
```

Allowed agent values for MVP:

```text
codex
claude
shell
```

### 2.5 Agent Launcher

The agent launcher converts user intent into tmux-backed remote commands.

Codex templates:

```bash
tmux new-session -As "aterm:codex:<workspace>" -c "<cwd>" "codex --no-alt-screen"
tmux new-session -As "aterm:codex:<workspace>:last" -c "<cwd>" "codex resume --no-alt-screen --last"
tmux new-session -As "aterm:codex:<workspace>:<id>" -c "<cwd>" "codex resume --no-alt-screen <session_id>"
```

Claude Code templates:

```bash
tmux new-session -As "aterm:claude:<workspace>" -c "<cwd>" "claude"
tmux new-session -As "aterm:claude:<workspace>:continue" -c "<cwd>" "claude --continue"
tmux new-session -As "aterm:claude:<workspace>:<id>" -c "<cwd>" "claude --resume <session_id>"
```

The launcher should treat CLI flags as data. Users must be able to override command templates per host or workspace because Codex and Claude Code evolve independently.

### 2.6 Local Session Registry

The local registry improves mobile UX without depending on agent internals.

Suggested tables:

```text
hosts
  id
  name
  hostname
  port
  username
  auth_type
  pinned_fingerprint
  default_cwd
  created_at
  updated_at

workspaces
  id
  host_id
  name
  remote_cwd
  preferred_agent
  codex_flags
  claude_flags
  created_at
  updated_at

agent_sessions
  id
  host_id
  workspace_id
  agent_type
  tmux_session_name
  agent_session_ref
  launch_command_label
  last_known_cwd
  last_attached_at
  created_at
  updated_at
```

`agent_session_ref` is optional. For Codex it can store a Codex session id or thread name if the user provides one. For Claude Code it can store a Claude session id if the user provides one. MVP should not depend on discovering these ids automatically.

### 2.7 Optional Remote Bootstrap

MVP can work without remote files. V1 can add a bootstrap script for better consistency.

Optional remote paths:

```text
~/.aterm/bin/aterm-agent
~/.aterm/config.toml
~/.aterm/sessions.jsonl
```

Bootstrap responsibilities:

- Check `tmux`, `codex`, and `claude` availability.
- Provide stable command wrappers.
- Record launch metadata.
- Avoid storing secrets.

The Android app must continue to work without bootstrap by falling back to direct tmux commands.

## 3. Data Flow

### 3.1 Connect to Host

1. User selects host.
2. App loads encrypted credentials.
3. App opens SSH connection.
4. App verifies host fingerprint against local pin.
5. App opens a control exec channel.
6. App runs capability checks:

```bash
command -v tmux
command -v codex
command -v claude
tmux -V
```

7. App opens PTY only after the user chooses shell, tmux session, or agent launch.

### 3.2 Start or Resume Codex

1. User selects workspace.
2. User taps Codex action.
3. App builds tmux session name.
4. App builds a quoted remote command.
5. App opens PTY channel.
6. App runs tmux attach-or-create command.
7. tmux starts or attaches to Codex.
8. Terminal renderer displays output.
9. Reading mode consumes scrollback snapshots for enhanced rendering.

### 3.3 Reconnect

1. SSH connection drops.
2. tmux keeps remote process alive.
3. App marks terminal as disconnected.
4. User reconnects or app reconnects.
5. App reopens SSH.
6. App attaches to the last tmux session.
7. Terminal resumes from tmux state.

### 3.4 Reading Mode

1. App captures terminal scrollback locally from the terminal buffer or remotely through `tmux capture-pane`.
2. Parser removes control noise.
3. Classifier detects blocks:

```text
markdown paragraph
heading
list
code fence
command invocation
command output
diff
approval prompt
plain terminal text
```

4. Compose renders blocks with mobile-optimized components.
5. User can jump back to raw terminal for exact state.

## 4. Agent Readability Parser

The parser must be conservative. It should improve common output while never hiding the raw terminal fallback.

Pipeline:

```text
Terminal snapshot
  -> ANSI sanitization
  -> line normalization
  -> block segmentation
  -> block classification
  -> mobile rendering
```

Classification rules for MVP:

- Lines beginning with `diff --git`, `+++`, `---`, `@@`, `+`, or `-` inside a diff block render as diff.
- Triple backtick fences render as code blocks.
- Shell prompts and command echo lines render as command blocks when confidently detected.
- Long contiguous output over a configured threshold renders collapsed.
- Unknown text renders as plain monospace or markdown-like text without destructive rewriting.

The parser should expose raw source for every rendered block so users can inspect exact terminal text.

## 5. Security Model

Security boundaries:

- Android app stores credentials locally.
- SSH remains the only required network protocol.
- Remote commands execute under the user's SSH account.
- tmux sessions are visible to that remote user.
- aTerminal must not require root on the remote host.

Required controls:

- Host key pinning.
- Encrypted private key storage.
- Confirmation before sending multiline paste.
- Confirmation before killing tmux sessions.
- Confirmation before running bootstrap install.
- No telemetry containing hostnames, commands, prompts, or terminal output unless the user explicitly opts in.

## 6. Error Handling

| Condition | User-facing behavior |
| --- | --- |
| `tmux` missing | Show install guidance and allow raw shell fallback |
| `codex` missing | Disable Codex actions for that host and show detection result |
| `claude` missing | Disable Claude actions for that host and show detection result |
| Host fingerprint changed | Block connection until user reviews and confirms |
| SSH drops | Show disconnected state and offer reconnect |
| tmux session missing on reconnect | Offer session list and workspace relaunch actions |
| Agent command exits | Keep tmux pane visible and show exit state |
| Reading parser fails | Fall back to raw terminal block |

## 7. Testing Strategy

### Unit Tests

- Shell quoting utility.
- tmux command builder.
- session name slug generation.
- agent command template rendering.
- ANSI sanitization.
- reading block segmentation.
- host fingerprint comparison.

### Integration Tests

- SSH connection to a local test container.
- tmux session creation and attach.
- Codex command template rendering without invoking external API.
- Claude command template rendering without invoking external API.
- reconnect to existing tmux session.

### UI Tests

- Add host flow.
- Host fingerprint confirmation.
- Workspace creation.
- Start Codex action.
- Resume Codex latest action.
- tmux session list.
- terminal quick key bar.
- reading mode renders markdown, code block, diff, and long output.

## 8. Future Architecture Options

### Remote Daemon

Add `aterm-agentd` only when MVP proves that direct SSH control is too limited.

Benefits:

- Better session inventory.
- Structured event stream.
- Cleaner command execution.
- Easier notification hooks.

Costs:

- Extra install step.
- More attack surface.
- More version compatibility work.

### Codex app-server

Codex exposes an experimental app-server. aTerminal can later support it as an optional integration, especially for structured session metadata and richer UI. It should not be required for MVP because tmux plus `codex resume` is more stable for a first release.

### Claude Code stream-json

Claude Code supports stream-json for print-mode workflows. This can power structured read-only or automation flows later. MVP interactive Claude Code should remain tmux-backed.

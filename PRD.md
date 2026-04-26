# aTerminal PRD

## 1. Product Summary

aTerminal is an Android SSH terminal optimized for agentic coding tools such as Codex and Claude Code. It keeps long-running agent sessions alive on a remote machine through tmux, provides fast resume flows for Codex and Claude Code, and improves mobile readability for agent responses, tool output, diffs, approvals, and long terminal scrollback.

The product is not a generic mobile terminal first. Its primary job is to make agent-driven coding usable from a phone when the actual development environment remains on a trusted remote machine.

## 2. Target Users

- Developers who already use Codex, Claude Code, or similar terminal-based coding agents.
- Developers who run coding agents on a remote workstation, VPS, homelab server, or WSL/Linux box.
- Users who need to monitor, resume, approve, interrupt, or redirect agent work while away from a desktop.
- Users who care more about reliable long-session control than full IDE features on mobile.

## 3. Core Problems

1. Mobile SSH terminals are designed for general shell usage, not agent workflows.
2. Codex and Claude Code sessions often run for a long time and must survive network drops.
3. Resuming the correct agent session is slow from a small touchscreen.
4. Agent terminal output is dense on mobile: markdown, diffs, command output, spinners, and approval prompts are hard to read.
5. Common agent controls, such as `Ctrl+C`, paste, approval, resume, project switching, and tmux attach, require too much typing.

## 4. Product Goals

- Start or resume Codex and Claude Code sessions in under three taps after SSH login.
- Keep agent processes alive when Android backgrounding or network changes disconnect the SSH channel.
- Provide a terminal mode that remains faithful to the remote PTY.
- Provide an agent reading mode that makes Codex and Claude Code output easier to scan on a phone.
- Use tmux and existing CLI resume features instead of requiring a custom remote service for MVP.
- Keep credentials and host trust secure by default.

## 5. Non-Goals

- Do not build a full mobile IDE in MVP.
- Do not replace Codex or Claude Code session storage.
- Do not depend on private or unstable internal storage formats for Codex or Claude Code.
- Do not require a remote daemon for MVP.
- Do not implement collaborative multi-user terminal sharing in MVP.
- Do not attempt perfect semantic parsing of every possible TUI frame in MVP.

## 6. MVP Feature Scope

### 6.1 SSH Host Management

Users can add, edit, and connect to SSH hosts.

Required fields:

- Display name
- Hostname or IP
- Port
- Username
- Authentication method: private key first, password optional later
- Optional default working directory

Security requirements:

- Private keys are stored through Android Keystore-backed encryption.
- Host fingerprints are shown on first connect and pinned after acceptance.
- Host fingerprint changes require explicit confirmation.

### 6.2 Terminal Session

Users can open an interactive SSH terminal backed by a PTY.

Required behavior:

- ANSI color support.
- Resizes remote PTY when Android orientation or viewport changes.
- Supports paste with multiline confirmation.
- Provides quick keys for `Esc`, `Tab`, `Ctrl+C`, `Ctrl+D`, arrow keys, and tmux prefix.
- Supports reconnecting to the same tmux session after SSH reconnect.

### 6.3 tmux Session Management

Users can manage remote tmux sessions from a mobile-first UI.

MVP actions:

- List tmux sessions.
- Attach to an existing tmux session.
- Create an agent tmux session.
- Detach from current tmux session.
- Rename a session created by aTerminal.
- Kill a session after confirmation.

Session naming convention:

```text
aterm:<agent>:<workspace-slug>
aterm:<agent>:<workspace-slug>:<short-id>
```

Examples:

```text
aterm:codex:aterminal
aterm:claude:backend-api
```

### 6.4 Codex Session Management

Users can start and resume Codex quickly.

MVP actions:

- Start Codex in a selected workspace.
- Resume the latest Codex session in a selected workspace.
- Resume a specific Codex session by id or thread name.
- Start Codex with an initial prompt.

Command templates:

```bash
tmux new-session -As "aterm:codex:<workspace>" -c "<cwd>" "codex --no-alt-screen"
tmux new-session -As "aterm:codex:<workspace>:last" -c "<cwd>" "codex resume --no-alt-screen --last"
tmux new-session -As "aterm:codex:<workspace>:<id>" -c "<cwd>" "codex resume --no-alt-screen <session_id>"
```

Codex-specific UX:

- Show a "Resume latest" action prominently.
- Show a "Resume by session id" action for advanced users.
- Offer `--no-alt-screen` by default to preserve tmux scrollback and improve mobile reading.
- Allow users to override default Codex flags per host or workspace.

### 6.5 Claude Code Session Management

Users can start and resume Claude Code quickly.

MVP actions:

- Start Claude Code in a selected workspace.
- Continue the most recent conversation in the current directory.
- Resume a specific Claude Code session by id.
- Start Claude Code with an initial prompt.

Command templates:

```bash
tmux new-session -As "aterm:claude:<workspace>" -c "<cwd>" "claude"
tmux new-session -As "aterm:claude:<workspace>:continue" -c "<cwd>" "claude --continue"
tmux new-session -As "aterm:claude:<workspace>:<id>" -c "<cwd>" "claude --resume <session_id>"
```

Claude-specific UX:

- Show a "Continue latest" action prominently.
- Show a "Resume by session id" action.
- Allow optional launch flags such as `--permission-mode`, `--model`, and `--append-system-prompt`.

### 6.6 Workspaces

Users can save remote project directories as workspaces.

Workspace fields:

- Name
- Host id
- Remote cwd
- Preferred agent: Codex, Claude Code, or none
- Default tmux session name
- Optional launch flags per agent

Workspace actions:

- Open shell here.
- Start Codex here.
- Resume Codex latest here.
- Start Claude here.
- Continue Claude here.

### 6.7 Agent Reading Mode

Users can switch from raw terminal view to agent reading mode.

MVP rendering improvements:

- Strip common ANSI control noise while preserving meaningful color.
- Reflow markdown paragraphs to mobile width.
- Render code fences as horizontally scrollable blocks.
- Collapse long command output by default.
- Highlight diffs with file headers, additions, deletions, and hunks.
- Render approval prompts as sticky action cards when detectable.
- Preserve raw terminal fallback for any output the parser cannot classify.

Reading mode is a client-side view over captured terminal content. It must not alter the remote process or send hidden input.

## 7. Key User Journeys

### Journey A: Resume Codex From Phone

1. User opens aTerminal.
2. User selects a saved host.
3. User selects a workspace.
4. User taps "Resume Codex latest".
5. App connects over SSH, attaches or creates a tmux session, and runs `codex resume --no-alt-screen --last`.
6. User sees the Codex session in terminal mode and can switch to reading mode.

Success criteria: the user resumes a Codex session without typing a shell command.

### Journey B: Monitor a Running Agent

1. User opens a host.
2. App lists active tmux sessions.
3. User taps an existing `aterm:codex:*` or `aterm:claude:*` session.
4. App attaches to the session.
5. User reviews progress in reading mode.
6. User sends a prompt or approval from the quick action bar.

Success criteria: the agent process survives prior mobile disconnects and is still reachable.

### Journey C: Start Claude Code in a Project

1. User selects a workspace.
2. User taps "Start Claude".
3. App creates or attaches a tmux session in the workspace cwd.
4. App launches `claude`.
5. User interacts through terminal mode.

Success criteria: the user starts Claude Code without manually typing `cd`, `tmux`, or `claude`.

## 8. Success Metrics

- Time from app open to resumed Codex session: under 15 seconds on an already configured host.
- Taps from workspace list to resumed agent: 3 or fewer.
- Agent process survives SSH disconnect in 100% of tmux-backed MVP flows.
- Reading mode can classify markdown, command output, and diffs in common Codex/Claude transcripts.
- No unpinned host fingerprint connections after first trust decision.

## 9. Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Codex or Claude CLI flags change | Resume flows break | Store command templates as configurable per host/workspace |
| TUI output is difficult to parse | Reading mode quality varies | Keep raw terminal as source of truth and reading mode as best-effort |
| Android background kills SSH | Lost foreground connection | Rely on tmux for process survival and reconnect to tmux |
| SSH key storage bugs | Credential exposure | Use Android Keystore-backed encrypted storage |
| tmux unavailable on remote host | Agent persistence unavailable | Detect missing tmux and show install guidance |
| Remote shell quoting issues | Commands fail for paths with spaces | Quote cwd and command arguments with a dedicated remote shell quoting utility |

## 10. Release Plan

### MVP 0: Terminal Foundation

- SSH host management.
- PTY terminal.
- tmux attach and detach.
- Quick key bar.

### MVP 1: Agent Launch

- Workspaces.
- Codex start and resume.
- Claude start and resume.
- tmux session list with agent labels.

### MVP 2: Reading Mode

- Scrollback capture.
- Markdown reflow.
- Code block rendering.
- Diff highlighting.
- Long output collapse.

### V1

- Remote bootstrap script.
- Configurable command templates.
- Session metadata registry.
- Android notifications for long-running sessions.
- Export/import host and workspace config without private keys.

### V2

- Optional remote daemon.
- Deeper Codex app-server integration.
- Claude stream-json integration for non-interactive workflows.
- Cross-device encrypted sync.

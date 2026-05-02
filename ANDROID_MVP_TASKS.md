# aTerminal Android MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Android MVP for an SSH terminal optimized for Codex and Claude Code sessions through tmux.

**Architecture:** The Android app is the SSH client, terminal renderer, session manager, and reading-mode UI. The remote host runs tmux plus Codex or Claude Code; no remote daemon is required for MVP.

**Tech Stack:** Kotlin, Jetpack Compose, Room, DataStore, Android Keystore, SSHJ or Apache Mina SSHD, JUnit, Compose UI tests.

---

## 1. Assumed Android Project Structure

The implementation should create a standard Android app project with focused modules. Keep files small and avoid large god objects.

```text
app/src/main/java/com/aterminal/app/
  AterminalApp.kt
  MainActivity.kt
  data/
    AppDatabase.kt
    HostEntity.kt
    WorkspaceEntity.kt
    AgentSessionEntity.kt
  security/
    SecretStore.kt
    HostFingerprintStore.kt
  ssh/
    SshClientFactory.kt
    SshConnection.kt
    SshControlChannel.kt
    SshPtyChannel.kt
  terminal/
    TerminalBuffer.kt
    TerminalSessionViewModel.kt
    TerminalScreen.kt
    QuickKeyBar.kt
  tmux/
    ShellQuoter.kt
    TmuxCommandBuilder.kt
    TmuxSession.kt
    TmuxRepository.kt
  agents/
    AgentType.kt
    AgentLaunchRequest.kt
    AgentCommandBuilder.kt
    AgentSessionRepository.kt
  readability/
    AnsiSanitizer.kt
    AgentOutputBlock.kt
    AgentOutputParser.kt
    ReadingModeScreen.kt
  hosts/
    HostListScreen.kt
    HostEditScreen.kt
    HostRepository.kt
  workspaces/
    WorkspaceListScreen.kt
    WorkspaceEditScreen.kt
    WorkspaceRepository.kt
```

Suggested tests:

```text
app/src/test/java/com/aterminal/app/
  tmux/ShellQuoterTest.kt
  tmux/TmuxCommandBuilderTest.kt
  agents/AgentCommandBuilderTest.kt
  readability/AnsiSanitizerTest.kt
  readability/AgentOutputParserTest.kt
```

## 2. MVP Milestones

### Milestone 1: Android Project Foundation

- [ ] Create a Kotlin Android project with Jetpack Compose enabled.
- [ ] Add dependencies for Room, DataStore, coroutines, lifecycle viewmodels, and the selected SSH library.
- [ ] Add a package structure matching the architecture above.
- [ ] Add a basic navigation shell with Host List, Workspace List, Terminal, and Settings destinations.
- [ ] Add unit test and Compose UI test scaffolding.
- [ ] Commit with `chore(android): scaffold app project`.

Acceptance criteria:

- App launches to an empty Host List screen.
- Unit tests run from Gradle.
- Package boundaries exist before feature work begins.

### Milestone 2: Local Data Model

- [ ] Implement `HostEntity` with host display name, hostname, port, username, auth type, pinned fingerprint, and timestamps.
- [ ] Implement `WorkspaceEntity` with host id, workspace name, remote cwd, preferred agent, and agent flags.
- [ ] Implement `AgentSessionEntity` with host id, workspace id, agent type, tmux session name, optional agent session ref, and last attached timestamp.
- [ ] Implement Room DAOs for hosts, workspaces, and agent sessions.
- [ ] Implement repositories that expose suspend functions and flows.
- [ ] Add unit tests for repository create, update, delete, and list behavior.
- [ ] Commit with `feat(data): add host workspace and session models`.

Acceptance criteria:

- Host, workspace, and agent session metadata can be persisted locally.
- The UI can observe host and workspace lists as flows.

### Milestone 3: Secure Host Credentials

- [ ] Implement `SecretStore` with Android Keystore-backed encryption.
- [ ] Store private keys separately from non-sensitive host metadata.
- [ ] Implement host fingerprint storage and comparison.
- [ ] Add confirmation state for unknown fingerprints and changed fingerprints.
- [ ] Add tests for fingerprint matching, mismatch detection, and missing fingerprint behavior.
- [ ] Commit with `feat(security): add encrypted ssh credential storage`.

Acceptance criteria:

- Private keys are not stored in plain Room tables.
- Changed host fingerprints block connection until the user confirms.

### Milestone 4: SSH Connection Layer

- [ ] Implement `SshClientFactory` to create configured SSH clients.
- [ ] Implement `SshConnection` for connect, disconnect, keepalive, and auth.
- [ ] Implement `SshControlChannel` for non-interactive commands.
- [ ] Implement `SshPtyChannel` for interactive PTY sessions.
- [ ] Add connection state reporting: disconnected, connecting, connected, failed.
- [ ] Add a local integration test plan using a disposable SSH container.
- [ ] Commit with `feat(ssh): add ssh connection and channel layer`.

Acceptance criteria:

- App can establish an SSH connection using a saved host.
- App can run a simple remote command over a control channel.
- App can open a PTY channel for terminal interaction.

### Milestone 5: Terminal View

- [ ] Implement terminal buffer abstraction with screen text and scrollback.
- [ ] Integrate the PTY channel with terminal input and output streams.
- [ ] Render terminal text in Compose.
- [ ] Implement viewport resize propagation to the remote PTY.
- [ ] Implement copy and paste.
- [ ] Add multiline paste confirmation.
- [ ] Implement `QuickKeyBar` with `Esc`, `Tab`, arrows, `Ctrl+C`, `Ctrl+D`, and tmux prefix.
- [ ] Commit with `feat(terminal): add interactive ssh terminal`.

Acceptance criteria:

- User can type into a remote shell.
- User can paste multiline text only after confirmation.
- Quick keys send the expected control sequences.

### Milestone 6: tmux Command Builder

- [ ] Implement `ShellQuoter` for single-quoted POSIX shell arguments.
- [ ] Add tests for spaces, quotes, empty strings, colons, and path-like values.
- [ ] Implement `TmuxCommandBuilder` for list, has-session, attach, new-session, kill, detach, and capture-pane.
- [ ] Add tests that verify exact generated commands.
- [ ] Implement session name slug generation for workspace names.
- [ ] Commit with `feat(tmux): add tmux command builder`.

Acceptance criteria:

- All tmux commands are generated through tested builders.
- No feature code manually concatenates unquoted remote shell arguments.

### Milestone 7: tmux Session Management UI

- [ ] Implement `TmuxRepository` using `SshControlChannel`.
- [ ] Parse `tmux list-sessions` output into `TmuxSession`.
- [ ] Add a tmux session list screen for a connected host.
- [ ] Add attach action for an existing session.
- [ ] Add kill action with confirmation.
- [ ] Add detach action from the terminal screen.
- [ ] Commit with `feat(tmux): add session management UI`.

Acceptance criteria:

- User can see active tmux sessions.
- User can attach to a selected session.
- User cannot kill a session without confirmation.

### Milestone 8: Agent Command Builder

- [ ] Implement `AgentType` with `codex`, `claude`, and `shell`.
- [ ] Implement `AgentLaunchRequest` with agent type, workspace cwd, tmux session name, optional agent session ref, optional initial prompt, and extra flags.
- [ ] Implement Codex command generation for start, resume latest, and resume by session id.
- [ ] Implement Claude Code command generation for start, continue latest, and resume by session id.
- [ ] Add tests for all command templates.
- [ ] Commit with `feat(agents): add codex and claude launch commands`.

Acceptance criteria:

- Codex launch commands include `--no-alt-screen` by default.
- Resume-by-id commands quote user-provided session ids.
- Claude continue and resume commands are generated correctly.

### Milestone 9: Workspace Agent Actions

- [ ] Add workspace list screen actions: Shell, Start Codex, Resume Codex Latest, Start Claude, Continue Claude.
- [ ] Add advanced action sheet for Resume Codex by id and Resume Claude by id.
- [ ] Create or reuse tmux sessions when launching agent actions.
- [ ] Store launched sessions in `AgentSessionEntity`.
- [ ] Attach the terminal to the launched tmux session.
- [ ] Commit with `feat(agents): add workspace launch actions`.

Acceptance criteria:

- User can start Codex from a workspace without typing commands.
- User can resume latest Codex from a workspace.
- User can start Claude Code and continue Claude Code from a workspace.

### Milestone 10: Remote Capability Detection

- [ ] Run capability checks after connecting to a host.
- [ ] Detect `tmux`, `codex`, and `claude` using `command -v`.
- [ ] Detect tmux version using `tmux -V`.
- [ ] Disable unavailable agent actions with clear messages.
- [ ] Show tmux install guidance when tmux is missing.
- [ ] Commit with `feat(hosts): detect remote agent capabilities`.

Acceptance criteria:

- Codex actions are disabled if `codex` is unavailable.
- Claude actions are disabled if `claude` is unavailable.
- tmux-backed actions are disabled if `tmux` is unavailable.

### Milestone 11: Reading Mode Parser

- [ ] Implement `AnsiSanitizer` to remove control noise while preserving readable text.
- [ ] Implement `AgentOutputBlock` types for plain text, heading, list, code, command, output, diff, approval, and unknown.
- [ ] Implement block segmentation for markdown, fenced code, diffs, and long output.
- [ ] Add parser tests using representative Codex and Claude transcript snippets.
- [ ] Keep raw source text attached to every parsed block.
- [ ] Commit with `feat(readability): parse agent terminal output`.

Acceptance criteria:

- Parser identifies code fences, diffs, and long output.
- Parser falls back to unknown/plain blocks instead of dropping text.
- Tests cover mixed markdown, command output, and diffs.

### Milestone 12: Reading Mode UI

- [ ] Add a terminal toolbar toggle between Terminal and Reading modes.
- [ ] Render headings, paragraphs, lists, code blocks, command blocks, output blocks, and diffs.
- [ ] Collapse long output with expand control.
- [ ] Provide "copy raw" for each block.
- [ ] Provide "jump to terminal" to return to raw PTY view.
- [ ] Commit with `feat(readability): add mobile reading mode`.

Acceptance criteria:

- User can switch between raw terminal and reading mode.
- Diffs are visually distinguishable.
- Long output does not dominate the mobile screen by default.

### Milestone 13: Reconnect Flow

- [ ] Track the last attached tmux session per host.
- [ ] Detect SSH disconnection and show a reconnect state.
- [ ] Reconnect to host using saved credentials.
- [ ] Reattach to the last tmux session when available.
- [ ] If the last session is missing, show tmux session list and workspace actions.
- [ ] Commit with `feat(ssh): add tmux reconnect flow`.

Acceptance criteria:

- Agent process remains alive after SSH disconnect because it is tmux-backed.
- User can reattach without retyping the command.

### Milestone 14: MVP Hardening

- [x] Add user-facing error messages for SSH auth failure, host key mismatch, missing tmux, missing Codex, missing Claude, and failed tmux command.
- [x] Add settings for Codex default flags and Claude default flags.
- [x] Add export/import for non-secret host and workspace metadata.
- [x] Add privacy note explaining that terminal content stays on device and remote host unless user exports logs.
- [x] Run unit tests and UI smoke tests.
- [x] Commit with `chore(mvp): harden android agent terminal flows`.

Acceptance criteria:

- Common failure states are understandable.
- Users can customize agent launch flags.
- No secret material is included in exported metadata.

### Milestone 15: Host Management UI

- [x] Add a Host list route backed by the local Room host repository.
- [x] Add an Add Host dialog for non-secret SSH host metadata.
- [x] Validate display name, hostname, username, and SSH port before saving.
- [x] Render saved hosts with auth method, connection target, Connect, and Delete actions.
- [x] Add ViewModel unit tests and Compose UI smoke tests.
- [x] Commit with `feat(hosts): add host management UI`.

Acceptance criteria:

- User can save SSH host metadata from the Hosts tab.
- Saved hosts are listed on the default screen.
- Invalid host input shows an actionable validation message.

### Milestone 16: Host Credential Capture

- [x] Add password and private-key fields to the Add Host dialog.
- [x] Save host credentials through Android Keystore-backed `SecretStore`.
- [x] Store private-key passphrases separately from Room metadata.
- [x] Delete stored credentials when a host is deleted.
- [x] Add unit tests for encrypted password/private-key credential storage.
- [x] Commit with `feat(security): capture host credentials`.

Acceptance criteria:

- Passwords, private keys, and passphrases are not stored in Room.
- Adding a host persists the selected auth credential after metadata creation.
- Deleting a host removes its stored credential material.

### Milestone 17: Host Connect Action

- [x] Wire the Host list Connect action to stored credentials.
- [x] Map saved password/private-key secrets into `SshAuthCredential`.
- [x] Add an injectable `HostConnector` backed by `SshConnection`.
- [x] Show connecting, connected, missing credential, and connection failure states.
- [x] Add ViewModel and Compose smoke tests for connect actions.
- [x] Commit with `feat(ssh): connect saved hosts`.

Acceptance criteria:

- Pressing Connect retrieves the saved host credential.
- Missing credentials block connection with a clear message.
- SSH connection failures are shown as user-facing errors.

### Milestone 18: Host Remote Capability Display

- [x] Return detected `tmux`, `codex`, and `claude` capabilities from a successful host connection.
- [x] Store detected capabilities per host in the Host list state.
- [x] Render tmux version, Codex availability, and Claude availability on the connected host card.
- [x] Show tmux install guidance when the remote host is missing tmux.
- [x] Add ViewModel and Compose smoke tests for capability display.
- [x] Commit with `feat(hosts): show remote capabilities after connect`.

Acceptance criteria:

- Pressing Connect runs remote capability detection after SSH authentication succeeds.
- The connected host card shows whether tmux, Codex, and Claude Code are available.
- Missing tmux is called out before the user tries a tmux-backed agent action.

### Milestone 19: Active SSH Session and tmux Sessions Route

- [x] Add an `ActiveHostSession` abstraction that retains the connected SSH session.
- [x] Store the active host session after a successful Host Connect action.
- [x] Expose the active session through an application-level store.
- [x] Wire the Sessions tab to create a `TmuxRepository` from the active session control channel.
- [x] Auto-refresh tmux sessions when a host session becomes active.
- [x] Add unit and Compose smoke tests for active session state and route wiring.
- [x] Commit with `feat(tmux): list sessions from active ssh host`.

Acceptance criteria:

- After connecting to a host, the app retains the SSH connection for downstream tmux actions.
- Opening the Sessions tab uses the active SSH session instead of the previous placeholder state.
- With no connected host, the Sessions tab still shows the existing connect prompt.

### Milestone 20: tmux Attach to Terminal PTY

- [x] Add a `TmuxPtyAttacher` that opens an SSH PTY and writes a quoted `tmux attach-session` command.
- [x] Add a terminal session sink so the shared Terminal ViewModel can receive attached PTY channels.
- [x] Route Sessions tab Attach actions through PTY attach when an active SSH host exists.
- [x] Navigate to the Terminal tab after a tmux session is attached.
- [x] Close the previously attached PTY when attaching a new tmux session.
- [x] Add unit and Compose smoke tests for PTY attach behavior.
- [x] Commit with `feat(terminal): attach tmux sessions to pty`.

Acceptance criteria:

- Tapping Attach on a tmux session opens an interactive PTY instead of running attach over the control channel.
- The Terminal tab receives the attached PTY channel and can stream/read/write through the existing terminal UI.
- The generated attach command quotes session names safely.

### Milestone 21: Workspace Agent Launch Route

- [x] Wire the Workspaces tab to the active host session.
- [x] Load saved workspaces for the connected host from the local repository.
- [x] Launch workspace agent actions by creating detached tmux sessions over the SSH control channel.
- [x] Attach launched tmux sessions to the shared Terminal PTY.
- [x] Store launched agent session metadata.
- [x] Add unit and Compose smoke tests for workspace launch routing.
- [x] Commit with `feat(workspaces): launch agents from active host`.

Acceptance criteria:

- After connecting to a host, the Workspaces tab shows that host's saved workspaces.
- Tapping Start Codex creates a detached tmux-backed Codex session and opens it in Terminal.
- Control-channel launch commands do not attempt interactive tmux attach.

## 3. Definition of Done for MVP

- User can save an SSH host and connect.
- User can open a remote shell.
- User can list and attach tmux sessions.
- User can start Codex in a workspace.
- User can run `codex resume --no-alt-screen --last` through a one-tap action.
- User can run `codex resume --no-alt-screen <session_id>` through a form.
- User can start Claude Code in a workspace.
- User can run `claude --continue` through a one-tap action.
- User can run `claude --resume <session_id>` through a form.
- User can switch to reading mode for agent output.
- SSH disconnect does not kill tmux-backed agent processes.
- Private keys are encrypted at rest.
- Host fingerprint changes are blocked until confirmed.

## 4. Initial Git Commit Sequence

Use these commits to keep review boundaries small:

```text
chore(android): scaffold app project
feat(data): add host workspace and session models
feat(security): add encrypted ssh credential storage
feat(ssh): add ssh connection and channel layer
feat(terminal): add interactive ssh terminal
feat(tmux): add tmux command builder
feat(tmux): add session management UI
feat(agents): add codex and claude launch commands
feat(agents): add workspace launch actions
feat(hosts): detect remote agent capabilities
feat(readability): parse agent terminal output
feat(readability): add mobile reading mode
feat(ssh): add tmux reconnect flow
chore(mvp): harden android agent terminal flows
```

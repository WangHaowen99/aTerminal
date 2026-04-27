# SSH Integration Test Plan

This plan verifies the real SSHJ transport against a disposable local SSH server.

## Container

Use a throwaway OpenSSH container with password auth enabled:

```bash
docker run --rm --name aterminal-ssh-test \
  -p 2222:22 \
  -e USER_NAME=agent \
  -e USER_PASSWORD=agentpass \
  lscr.io/linuxserver/openssh-server:latest
```

## Manual Checks

1. Create a host in the app with hostname `10.0.2.2` on Android Emulator, or the workstation LAN IP on a phone, port `2222`, username `agent`, and password `agentpass`.
2. Connect through `SshConnection` and verify state moves `Disconnected -> Connecting -> Connected`.
3. Run a control command through `SshControlChannel`: `printf aterminal-ok`.
4. Open a PTY through `SshPtyChannel`, write `printf pty-ok\n`, and verify output arrives in the terminal reader.
5. Resize the terminal viewport and verify no exception is thrown while the PTY remains open.
6. Disconnect and verify state returns to `Disconnected`.

## Failure Checks

1. Stop the container and verify connect reports `SshConnectionState.Failed`.
2. Restart the container with a new host key and verify the fingerprint flow blocks until user confirmation.

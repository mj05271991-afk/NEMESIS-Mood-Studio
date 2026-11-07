# Moon Studio release signing setup

HMG-API signs mods with an ECDSA P-256 author key. The private key must stay outside Git and be backed up securely. Its public key determines the mod's UUID, which must remain stable for updates.

## Generate the author key locally (Linux, macOS, or GitHub Codespaces)

```sh
git clone https://github.com/VysseCodes/HMG-API.git
cd HMG-API
mkdir -p out/engine-host
javac --release 8 -d out/engine-host core/runtime/java/hmgapi/engine/*.java
umask 077
java -cp out/engine-host hmgapi.engine.Signing keygen moon-studio-author.key
java -cp out/engine-host hmgapi.engine.Signing uuid moon-studio-author.key.pub
```

Keep both key files secure; **never commit or upload them as repository artifacts**. Save the displayed UUID for identifying future releases.

## Configure the existing GitHub Actions workflow

The workflow expects repository Actions secret `HMGAPI_SIGNING_KEY_BASE64`. The keygen file contains base64-encoded PKCS#8 DER on a single line; put **that file's content** into the secret, without encoding it a second time.

In the GitHub repository: Settings → Secrets and variables → Actions → New repository secret. Name it `HMGAPI_SIGNING_KEY_BASE64` and paste the exact private-key file contents. Never paste the private key into issues, chat, commits, or logs.

## Build and verify

Run `Build Moon Native Audio and Android Integration` on branch `fix/audio-loopback-ui`. Confirm the job's signed packaging step runs and the `Moon-Studio-SIGNED-HMGAPI-NEMESIS-UNTESTED` artifact appears. Inspect its structure before any in-vehicle test.

A successful build verifies packaging and signature mechanics only. It does **not** establish compatibility with a particular EV6 firmware, access to vehicle audio, or working ambient lighting. Test while parked.

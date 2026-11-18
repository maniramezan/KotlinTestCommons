# Preparing and publishing a release

CI stages and verifies Maven publications on every PR. [release-please](https://github.com/googleapis/release-please)
keeps a release PR open on `main` with the next version and its release notes. Merging that PR is the
only way to publish; an ordinary push to `main` cannot publish to Maven Central or create a tag.

## Release sequence

1. Merge reviewed changes through PRs. After CI passes on `main`, the Release workflow opens or updates
   a PR titled `chore: release <version>`. It raises `.release-please-manifest.json` and every version
   pin in the documentation, and its description holds the release notes.
2. Review the release PR: its version, notes, compatibility limits, and public API changes. CI runs on it
   like any other PR. Merge it when you are ready to release.
3. After CI passes on the merge commit, the Release workflow stages every Maven publication from that
   commit, verifies a separate consumer against the staged files, and publishes to Maven Central.
4. A separate job waits for all manifest files to be downloadable and runs consumers against Maven
   Central. Only then does release-please create the bare SemVer tag and the GitHub Release from the
   release PR's notes, and open the next release PR if releasable commits are already waiting.

`feat` selects a minor version; `fix` and `perf` select a patch before 1.0. Other commit types do not
release and are left out of the notes. Breaking changes select a minor version before 1.0 and a major
version afterward. Tags are bare SemVer, such as `0.1.1`. To choose a version yourself, add a
`Release-As: 0.4.0` footer to a commit on `main`; release-please proposes that version in the release PR.

Never edit `.release-please-manifest.json` or the version pins between `x-release-please` markers by
hand; the release PR updates both. `python3 Scripts/check-doc-versions.py` fails any PR whose
documented pins differ from the manifest. Wrap a new documented pin in
`<!-- x-release-please-start-version -->` and `<!-- x-release-please-end -->` lines and list its file
under `extra-files` in `release-please-config.json`.

Preserve existing release tags and artifacts. Published versions are immutable; fixes belong in a new version.
Document behavioral changes and migration notes in PR descriptions and GitHub Release notes, not a
changelog file.

## Verify without publishing

For the same staging checks locally, use the [independent consumer instructions](../verification/consumer/README.md).
Run the release-script unit tests with:

```sh
python3 -m unittest discover -s Scripts/tests -p 'test_*.py'
```

The candidate manifest requires metadata, binaries, sources, and documentation for all 13 publications.
The consumer verifies a framework-free core runtime, JUnit 4 without Jupiter, consumer-supplied Jupiter,
Android API use, and both iOS compilation variants. It uses Maven coordinates rather than project
dependencies. Consult the [verification matrix](compatibility.md) for the runtime limits, especially iOS
Compose UI tests.

## Publishing credentials

Create these under **Repository secrets** in
[Settings → Secrets and variables → Actions](https://github.com/maniramezan/KotlinTestCommons/settings/secrets/actions).
The publishing job uses an environment named `secrets`, but it can read repository secrets. An empty
Environment secrets section is fine when these repository secrets exist. Keep one authoritative value
for each name; an environment secret with the same name takes precedence.

| Repository secret | Value |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user-token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user-token password |
| `SIGNING_IN_MEMORY_KEY` | Complete ASCII-armored private GPG key, with its original line breaks |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | GPG key passphrase, if the key is protected |
| `RELEASE_PLEASE_TOKEN` | Fine-grained personal access token for release-please; see below |

The Release workflow fails with the missing secret's name before calling release-please or attempting an upload. The passphrase
secret is optional for an unprotected key. No secret values are printed by the preflight check.

### release-please token

release-please opens the release PR with this token instead of `GITHUB_TOKEN`, because GitHub does not
run CI on PRs opened by `GITHUB_TOKEN`. Create a
[fine-grained personal access token](https://github.com/settings/personal-access-tokens/new) limited to
this repository, with **Contents**, **Pull requests**, and **Issues** (for release labels) set to
read and write. Give it an expiration and store it:

```sh
gh secret set RELEASE_PLEASE_TOKEN --repo maniramezan/KotlinTestCommons
```

Renew it before it expires; until then, the Release workflow fails at its first step.

### Central Portal account and token

Sign in to [Central Portal](https://central.sonatype.com/) as the GitHub user owning the namespace.
Confirm `io.github.maniramezan` is verified; this library's group is
`io.github.maniramezan.testcommons`. GitHub sign-in normally provisions the personal namespace.
See [namespace registration](https://central.sonatype.org/register/namespace/) if it is absent.

At [User Tokens](https://central.sonatype.com/usertoken), generate a publishing token with a descriptive
name and expiration. Store its generated username/password in the two Maven secrets. These are the
token values, rather than the interactive account login credentials. The values are displayed only
once. See [token generation](https://central.sonatype.org/publish/generate-portal-token/).

### GPG key and fingerprint

An existing publishing key can be reused. Otherwise create one interactively:

```sh
gpg --full-generate-key
```

Choose an RSA signing key, an expiration, and a passphrase. List the key and copy its full fingerprint:

```sh
gpg --list-secret-keys --keyid-format LONG
```

For this example, the fingerprint is the second line's complete hexadecimal value:

```text
sec   rsa4096/1234567890ABCDEF
      ABCDEF1234567890ABCDEF1234567890ABCDEF12
```

Use the fingerprint in commands. Neither line is the private-key secret. Publish the public key so
Central can verify signatures:

```sh
gpg --keyserver hkps://keyserver.ubuntu.com --send-keys YOUR_FINGERPRINT
```

Export the private key directly into the repository secret:

```sh
gpg --armor --export-secret-keys YOUR_FINGERPRINT |
  gh secret set SIGNING_IN_MEMORY_KEY --repo maniramezan/KotlinTestCommons
```

Store the unlocking passphrase separately as `SIGNING_IN_MEMORY_KEY_PASSWORD`. The private-key value
must include the complete `BEGIN PGP PRIVATE KEY BLOCK` through `END PGP PRIVATE KEY BLOCK` text.
Keep private keys and token values out of commits and chat. See the
[signing requirements](https://central.sonatype.org/publish/requirements/gpg/) and
[publisher plugin instructions](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#in-memory-gpg-key).

## Propagation and recovery

The publishing task may finish while Central is still distributing files. The plugin documents a
[10–30 minute availability window](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#publishing-releases).
The public-verification job retries missing files for up to 30 minutes and then runs the independent
consumer. It does not equate an accepted deployment with a downloadable release.

If public verification fails after publishing succeeded, wait for Central or inspect the deployment
status, then use **Re-run failed jobs** on that same workflow run. The successful publishing job is
retained, so verification can retry without uploading the immutable version again. Do not re-run all
jobs; Central rejects a second upload of the same version. The original run retains its release commit,
manifest, and version.

If tagging fails after public verification succeeded, rerun the failed job. release-please tags only
merged release PRs still labeled `autorelease: pending`, and the job then checks that the tag points
at the published commit. Until the tag exists, release-please does not open the next release PR.

For an already tagged version, verify its downloads without invoking publishing:

<!-- x-release-please-start-version -->
```sh
./gradlew -p verification/consumer verifyConsumer -PtestCommonsVersion=0.3.0 --refresh-dependencies
```
<!-- x-release-please-end -->

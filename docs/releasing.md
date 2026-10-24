# Preparing and publishing a release

CI stages and verifies Maven publications on every PR. After releasable changes reach `main`, the
Release workflow prepares a versioned candidate. Publishing is a separate, explicit action. Its
`publish` input defaults to `false`; an ordinary successful push to `main` cannot publish to Maven
Central or create a tag.

## Release sequence

1. Merge reviewed changes through PRs and wait for CI on the exact `main` commit to pass.
2. The Release workflow computes a version from Conventional Commits, stages every Maven publication,
   and verifies a separate consumer against that file repository. It uploads candidate evidence.
3. Review the candidate, its version, compatibility limits, public API changes, and release notes.
4. Run **Release** manually on `main` with `publish` selected, `force_version` set to the reviewed
   candidate version, and `candidate_commit` set to its full commit SHA. The run refuses a different
   `main` commit, requires successful CI for that commit, and repeats candidate verification before
   publishing. If `main` has moved, prepare and review the new candidate first.
5. After Central accepts the deployment, a separate job waits for all manifest files to be downloadable
   and runs consumers against Maven Central. Only then does another job create the tag and GitHub Release.

`feat` selects a minor version; `fix` and `perf` select a patch before 1.0. Other commit types do not
release. Breaking changes select a minor version before 1.0 and a major version afterward. Tags are
bare SemVer, such as `0.1.1`. `force_version` is optional when preparing and required when publishing,
so the published version is the one that was reviewed.

Preserve existing release tags and artifacts. `0.1.0` already exists; fixes belong in a new version.
Document behavioral changes and migration notes in PR descriptions and GitHub Release notes, not a
changelog file.

## Prepare without publishing

From **Actions → Release → Run workflow**, select `main`, leave `publish` unchecked, and optionally
enter an exact new `force_version`. Supply `force_version` when the commit history contains no
releasable changes since the latest tag. The run stages unsigned candidates locally and verifies their
Maven metadata and consumers. It needs no publishing credentials and creates no external publication or tag.

For the same checks locally, use the [independent consumer instructions](../verification/consumer/README.md).
Run the release-verification unit tests with:

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

The publishing job fails with the missing secret's name before attempting an upload. The passphrase
secret is optional for an unprotected key. No secret values are printed by the preflight check.

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
jobs or dispatch another publication of that version. The original run retains its candidate commit,
manifest, and version.

If tag/release creation fails after public verification succeeded, rerun the failed job. It accepts an
existing tag only when it already points to the verified commit and creates only a missing GitHub
Release. It never moves an existing tag.

For an already tagged version, verify its downloads without invoking publishing:

```sh
./gradlew -p verification/consumer verifyConsumer -PtestCommonsVersion=0.1.0 --refresh-dependencies
```

# Upstream provenance

This directory is a stripped vendor snapshot. Its original Git metadata and
submodule metadata were not retained, so release builds must record upstream
revisions here.

## ics-openvpn snapshot

- Project: https://github.com/schwabe/ics-openvpn
- Vendored app version reference: `0.7.64` (`versionCode 219`)
- The vendored OpenSSL tree before the local update matched the OpenSSL
  submodule used by ics-openvpn `master` on 2026-07-12.
- ics-openvpn revision checked on 2026-07-12:
  `f0792e99b00ef0df7e2e0e749acbcf3203eab587`
- Its OpenSSL submodule revision:
  `e0115652413ba9eef1f2847e2ad7a5c8f360aabd`
- Its OpenVPN 2 submodule revision:
  `a3f4dcdb7f749b5c10638ea0de64a3a8b1a9dab1`

The OpenSSL submodule revision above is OpenSSL 4.0.0 plus one ARM assembly
build fix. It had not yet incorporated OpenSSL 4.0.1 when checked.

## Local OpenSSL security update

Applied on 2026-07-12:

- Official project: https://github.com/openssl/openssl
- Source tag: `openssl-4.0.1`
- Source commit: `1e963a8680ec78ad2072792c7a1a71f3c530bd2e`
- Release archive SHA-256:
  `2db3f3a0d6ea4b59e1f094ace2c8cd536dffb87cdc39084c5afa1e6f7f37dd09`
- Security reference:
  https://mirror.openssl-library.org/news/vulnerabilities-4.0/

All files present in this stripped vendor tree that changed between the
official `openssl-4.0.0` and `openssl-4.0.1` tags were replaced with their
4.0.1 versions. Generated public headers and the ARM64 AES/SHA assembly outputs
were updated to match those sources.

`openssl/openssl.cmake` was also corrected so `crypto/ec/ecp_sm2p256.c` is
compiled only for `arm64-v8a`, alongside its ARM64 assembly implementation. The
file assumes 64-bit `BN_ULONG` limbs and is not part of OpenSSL's 32-bit ARM
source set. Compiling it for `armeabi-v7a` produced integer-truncation and
out-of-bounds-copy warnings even though the optimized method was not referenced
on that ABI.

## Recovery copy

The pre-update OpenSSL 4.0.0 vendor tree is archived at:

`/Users/sangchan/Documents/New project/third_party/backups/ics-openvpn-openssl-4.0.0-20260712.tar.gz`

Archive SHA-256:

`b54a55e544b5a6a5f51b418aa0f6c1b4cc62f554244cb6a67c81da0ef9c4bb42`

## Verification

The TV release bundle was rebuilt from a clean temporary app copy for both
`arm64-v8a` and `armeabi-v7a` with:

```sh
gradle --no-daemon :app:bundleTvRelease :app:lintTvRelease
```

Result: `BUILD SUCCESSFUL`, with Lint completing successfully. Both packaged
`libopenvpn.so` binaries report `OpenSSL 4.0.1 9 Jun 2026`. The 32-bit library
contains no `EC_GFp_sm2p256_method` or `ecp_sm2p256_*` optimized symbols; the
ARM64 library retains them.

Before a future release, compare this vendor tree with the current ics-openvpn,
OpenVPN, and OpenSSL security releases and repeat the two-ABI clean build and
device connection tests.

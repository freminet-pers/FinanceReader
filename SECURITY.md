# Security policy

FinanceReader is an open-source Android application that can send selected article text to a translation endpoint configured by the user. Please treat API keys and article content as sensitive.

## Reporting a vulnerability

Please do not publish credentials, private article text, or an exploitable proof of concept in a public issue.

Use GitHub's private vulnerability-reporting flow for this repository when it is available:

<https://github.com/freminet-pers/FinanceReader/security/advisories/new>

If that page is unavailable, contact the repository owner through the [freminet-pers GitHub profile](https://github.com/freminet-pers) and request a private reporting channel. Include the affected version, device/Android version when relevant, reproduction steps that do not contain secrets, and an impact assessment. Do not assume that a public issue is private.

This repository does not promise a response or fix timeline. Please allow maintainers reasonable time to investigate before public disclosure.

## If an API key may have been exposed

Revoke or rotate the key with its provider immediately, then remove it from local logs and any shared artifacts. Redacting a public issue does not guarantee that the value was never copied; a new key is safer.

## Scope notes

- Report vulnerabilities in FinanceReader code, release packaging, or repository automation here.
- Provider services, feed publisher sites, and the upstream Feeder project have their own reporting channels and policies.
- Build and test commands should use disposable credentials or no credentials at all. Never commit `local.properties`, provider keys, signed release material, or private feed exports.


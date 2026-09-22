# FinanceReader project scope

FinanceReader is a fork and extension of [Feeder](https://github.com/spacecowboy/Feeder), not an official Feeder distribution. This note makes the maintenance boundary explicit for users, contributors, and future releases.

## What this repository owns

The project-specific surface includes:

- the default finance feed list and its documentation;
- finance-oriented translation prompts and safeguards;
- title, RSS-description, and full-text translation workflows, including caching and provider-specific configuration;
- fork-specific branding, release packaging, bilingual documentation, and repository governance.

The implementation currently lives alongside the inherited Feeder code under the existing package and module structure. A path or package name containing `feeder` is not, by itself, a promise that the code is unchanged from upstream.

## What remains inherited or shared

RSS/Atom/JSON Feed parsing, the general local reader, offline storage, common UI patterns, OPML handling, and many build/dependency conventions originate in Feeder. Upstream history and attribution remain part of this repository. Changes that improve shared reader behavior should be described clearly as fork changes and checked for compatibility with the inherited code.

## Support and issue routing

- Report FinanceReader-specific behavior, translation configuration, packaged APKs, and default feeds in this repository.
- Consult [Feeder](https://github.com/spacecowboy/Feeder) only for upstream context or behavior that has not been changed here; upstream maintainers do not automatically support this fork.
- Do not include API keys, private article text, account data, or sensitive logs in issues or pull requests.

## Changelog provenance

`CHANGELOG.md` is intentionally retained as a single readable history. The `2.23.6` entry is FinanceReader-specific. Entries from `2.22.0` downward are retained Feeder release history from the fork base and keep their upstream links and contributor attribution. The file is not rewritten to erase that provenance.

For future entries:

1. Add FinanceReader changes under the next project version and use Keep a Changelog headings.
2. Link to the corresponding FinanceReader release or pull request when one exists.
3. Do not rewrite inherited Feeder entries; correct a provenance problem with a short note and a source link instead.

## Licensing and attribution

The repository is distributed under GPL-3.0. Keep the [LICENSE](../LICENSE) file, upstream notices, and dependency attributions when modifying or redistributing the project. Provider names and feed publishers remain their respective owners; mentioning a provider in documentation is not an endorsement.


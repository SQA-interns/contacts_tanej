# Release notes

> For: agent (writes), human (reads) · Written in: phase 7 (`/release`) · Agent: writes

## Delivered

## Known limitations

- **Spring Security 6.5.11 advisories (D-56 (c), F-V13), accepted by the developer per CVE because none is reachable:** CVE-2026-59270 (embedded UnboundID LDAP container; the app uses no LDAP), CVE-2026-41707 (DPoP proof replay; the app accepts plain Bearer Firebase ID tokens, DPoP is not active and a test shows `Authorization: DPoP ...` is refused with 401), CVE-2026-47841 (WebAuthn; the app has none). Spring Security 6.5 has no open-source fix (OSS support ended 2026-06-30; fixes are in 7.0.7 and 7.1.1, which need Spring Boot 4).
- **Next version:** move to Spring Boot 4.x / Spring Security 7.1.

## Must be tested manually by a human

## Decisions pending review

## Data retention duties

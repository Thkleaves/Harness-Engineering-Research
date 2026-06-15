# URL Shortener Defect Fixes

Date: 2026-06-15

## Scope

Fix 3 defects in the existing URL shortener, no new features. All changes within existing file set.

## D1 — Collision Retry

**Symptom:** `generateShortCode()` produces a random 7-char code but `createShortUrl` doesn't check whether the code already exists. A collision silently overwrites the old URL.

**Fix:** In `UrlService.createShortUrl`, loop: generate code → check existence via repository → retry if exists. Max 3 attempts. Exceeded → `ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)`.

**Files:** `service/UrlService.java`

## D2 — URL Format Validation

**Symptom:** `CreateUrlRequest` only has `@NotBlank`. `"not-a-url"` passes validation and gets stored.

**Fix:** Add `@URL` constraint on the `url` field. Jakarta `@URL` validates URL format, rejecting bare strings like `"not-a-url"`. Accepts any valid URL scheme (http, https, etc.). `@NotBlank` stays for the empty-string case. Controller already uses `@Valid` — no controller change needed.

**Files:** `dto/CreateUrlRequest.java`

## D3 — Stats Atomicity

**Symptom (false positive):** Concern that `incrementAccessCount` is not atomic with the resolve. Investigation shows `AtomicLong` already provides atomic increment; `ConcurrentHashMap.get` + `AtomicLong.incrementAndGet` is safe for eventual-consistency counter. No fix needed.

## Tests to Add

| Test | File | What |
|------|------|------|
| Collision retry succeeds | `UrlServiceTest.java` | Mock repo state so first code exists, second is free |
| Collision max retries exceeded | `UrlServiceTest.java` | All 3 generated codes already exist → exception |
| Invalid URL rejected (no protocol) | `UrlControllerTest.java` | `{"url":"not-a-url"}` → 400 |

| Short code contains only valid chars | `UrlServiceTest.java` | Verify all chars are in ALPHABET |

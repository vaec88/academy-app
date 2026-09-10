# JWT Security over WebFlux

| |                                                                                                                                       |
| --- |---------------------------------------------------------------------------------------------------------------------------------------|
| **Status** | Draft                                                                                                                                 |
| **Date** | 2026-09-09                                                                                                                            |
| **Scope** | `academy-app` (backend only)                                                                                                       |
| **Affects** | new `security/` package, new `LoginRestController`, the existing `User` vertical, `StudentRestController`, `pom.xml`, `application.yaml` |

> Specification only. Part 2 is written after the implementation.

---

# Part 1 — Specification

## Problem

Every endpoint is open. Anyone who reaches the port reads and writes everything, and there is nowhere to put a rule like "only ADMIN deletes a student".

The `.pathMatchers("/v1/**", "/v2/**").permitAll()` defined in `SecurityConfig.java` is for testing. It must be replaced for login path only.

What the project has today: no security dependency, no `security/` package, no `/login`. 

The `User` and `Role` verticals already exist (`model/User`, `model/Role`, `IUserRepository`, `IRoleRepository` and their services) but nothing uses them.

`GlobalErrorWebExceptionHandler` already maps `401` and `403` to `"Unauthorized access"` — a branch that is currently unreachable.

The application is reactive and stateless, so nothing session-based is an option.

## Goal

`POST /login` exchanges username and password for a signed JWT. Every other request must send `Authorization: Bearer <token>` or gets a `401`. The roles inside the token become Spring Security authorities, usable from path rules and from `@PreAuthorize`.

A user with ADMIN role can do all operations. A user with USER role can do all operations except deleting.

The chain is written by hand instead of using the resource-server starter: the point of the lesson is that every step is visible.

## Scope

**In scope**

* `spring-boot-starter-security` + `jjwt` 0.13.0 in the `pom.xml`.
* New `security/` package: `User`, `AuthRequest`, `AuthResponse`, `JwtUtil`, `AuthenticationManager`, `SecurityContextRepository`, `AuthValidator`, `WebSecurityConfig`.
* New `LoginRestController` with `POST /login`.
* Three additions to the existing `User` vertical: `findOneByUsername`, `searchByUser`, `saveHash`.
* `@EnableReactiveMethodSecurity` and one `@PreAuthorize` example on `StudentRestController.findAll`.
* The secret externalised as `jjwt.secret` / `JWT_SECRET`.

## How It Works

```
POST /login {username, password}
      |
      +-- searchByUser(username) --> model.User + role names --> security.User
      +-- BCrypt.checkpw(password, hash)   ->  false  ->  401
      +-- JwtUtil.generateToken(user)
      v
   200 { "access_token": "eyJ..." }


GET /v1/students    Authorization: Bearer eyJ...
      |
      +-- SecurityContextRepository.load()    reads the header, cuts off "Bearer "
      |         no header / wrong prefix  ->  401
      +-- AuthenticationManager.authenticate()   validates the token
      |         claim "roles" --> SimpleGrantedAuthority
      +-- authorizeExchange:  /login permitAll | anyExchange authenticated
      +-- @PreAuthorize (annotated controllers only)
      v
   200
```

Every failure goes through `GlobalErrorWebExceptionHandler`, which answers `{ datetime, status, message, path, errors }`.

## The New Classes

Written in this order — each one carries a `//Clase Sn` marker:

| # | Class                                      | What it does |
| --- |--------------------------------------------| --- |
| S1 | `security/User`                            | The `UserDetails`: `username`, `password` (`@JsonIgnore`), `enabled`, `List<String> roles`. `getAuthorities()` maps each role to `SimpleGrantedAuthority` |
| S2 | `security/AuthRequest`                     | The login body: `username` + `password` |
| S3 | `security/AuthResponse`                    | `record AuthResponse(@JsonProperty("access_token") String token)` |
| S4 | `security/JwtUtil`                         | `generateToken`, `getAllClaimsFromToken`, `getUsernameFromToken`, `validateToken` |
| S5 | `security/AuthenticationManager`           | `ReactiveAuthenticationManager`: token → `Authentication` with authorities |
| S6 | `security/SecurityContextRepository`       | `ServerSecurityContextRepository`: header → `SecurityContext`. `save()` returns `null`, nothing is stored |
| S7 | `config` additions                         | `@EnableWebFluxSecurity`, `@EnableReactiveMethodSecurity`, the `SecurityWebFilterChain` and the `BCryptPasswordEncoder` bean |
| S8 | `service` + `repository` additions         | `IUserRepository.findOneByUsername`, `IUserService.searchByUser` and `saveHash`. Comes after S1 (it returns `security.User`) and after S7 (it injects the encoder bean) |
| S9 | `controller/LoginRestController`           | `POST /login` — the first end-to-end test |
| S10 | `security/AuthValidator` + `@PreAuthorize` | Method security on a chain that already works |


## Changes to Existing Code

| File                               | Change |
|------------------------------------| --- |
| `pom.xml`                          | `spring-boot-starter-security`; `jjwt-api` (compile), `jjwt-impl` and `jjwt-jackson` (runtime) |
| `application.yaml`                 | the `jjwt.secret` property |
| `config/SecurityConfig`            | `@EnableWebFluxSecurity`, `@EnableReactiveMethodSecurity`, the `SecurityWebFilterChain` and the `BCryptPasswordEncoder` bean |
| `repository/IUserRepository`       | `Mono<User> findOneByUsername(String username)` |
| `service/IUserService`             | `searchByUser` and `saveHash` |
| `service/impl/UserServiceImpl`     | Both implementations, plus two injected beans: `IRoleRepository` and `BCryptPasswordEncoder` |
| `controller/StudentRestController` | `@PreAuthorize("@authValidator.isValid()")` on `findAll` |

Used with no change at all: `model/User`, `model/Role`, `IRoleRepository`. Nothing in `handler/`, `RouterConfig` or `exception/` is touched — the `/v2` routes are covered by `anyExchange()`.

`searchByUser` reads the user, then reads each role by id to get its name, and returns a `security.User`:

```java
userRepository.findOneByUsername(username)
    .zipWhen(user -> Flux.fromIterable(user.getRoles())
        .flatMap(role -> roleRepository.findById(role.getId()))
        .map(Role::getName)
        .collectList())
    .map(tuple -> new com.academy.security.User(...));
```

## Contract

**Login**

```
POST /login   { "username": "sysadmin", "password": "123456789" }
   200 -> { "access_token": "eyJ..." }
   401 -> empty body   (wrong password AND unknown user — they look identical)
```

The field is `access_token`, not `token`. Decoded payload:

```json
{ "roles": ["ADMIN"], "username": "sysadmin", "test-value": "sample-test-value",
  "sub": "sysadmin", "iat": 1755648000, "exp": 1755666000 }
```

**Every other endpoint** keeps its current contract and now requires the header:

| Situation | Status |
| --- | --- |
| Valid token | unchanged |
| No header, wrong prefix, or expired token | `401` + `{ datetime, message: "Unauthorized access" }` |
| Valid token but `@PreAuthorize` denies | `403` + same body |

## Configuration

```yaml
jjwt:
  secret: ${JWT_SECRET:4u7x!A%D*G-KaNdRgUkXp2s5v8y/B?E(H+MbQeShVmYq3t6w9z$C&F)J@NcRfUjW}
```
Read with `@Value("${jjwt.secret}")`

## Data

The exact documents are in **`.agents/features/security/resources/data.json`** (Extended JSON: `{"$oid": ...}` is an `ObjectId`). Load them into `academy`:

| username | password    | role |
| --- |-------------| --- |
| `sysadmin` | `123456789` | `ADMIN` |
| `participant` | `987654321` | `USER` |

Three things about this data:

* **The embedded role has `_id`, and `name`:** `roles: [ { _id: ObjectId("5e01..."), name: "ADMIN" } ]`.
* **`_id` is an `ObjectId`, the models declare `String id`.** Spring Data converts between them; nothing has to change.
* **The hash is inserted verbatim** and corresponds to `123`. It is a fixture, so `/login` works on the first run without ever calling `saveHash`. To change the password, generate the hash with `new BCryptPasswordEncoder().encode("...")`.

## Acceptance Criteria

* [ ] Before the change `GET /v1/students` answers `200` with no header; after it, `401`.
* [ ] `POST /login` with `sysadmin` / `123456789` returns `200` and `access_token`.
* [ ] A wrong password and an unknown user both return `401` with an empty body.
* [ ] The token decoded in jwt.io shows `sub`, `roles`, `username` and `exp` five hours after `iat`.
* [ ] `GET /v1/students` with the header returns the list; without it, `401` and the `"Unauthorized access"` body.
* [ ] Lowercase `bearer` and a missing prefix are both `401`.
* [ ] An edited or expired token is rejected.
* [ ] `POST /v2/students` — a functional route — is also protected, with no change to `RouterConfig`.
* [ ] The token for `participant` carries `["USER"]` and the one for `sysadmin` carries `["ADMIN"]`, read from documents whose embedded role has an `_id` and `name`.
* [ ] With `AuthValidator.isValid()` returning `false`, `findAll` gives `403` while `findById` still gives `200`.
* [ ] Swapping the annotation for `hasRole('ADMIN')` gives `403`, and `hasAuthority('ADMIN')` gives `200`.
* [ ] `./mvnw test` stays green with the existing tests untouched.

## Prerequisites

* MongoDB on `localhost:27017`, database `academy`, seeded from `resources/data.json`, and the application pointing at that same database.
* Java 25 and the Maven wrapper. Boot 4.1 brings Spring Security 7.1, so the reactive DSL is the lambda form, not `.and()`.
* A REST client that can set headers.

---

# Part 2 — Implementation from Spec

| |                                                     |
| --- |-----------------------------------------------------|
| **Status** | Implemented                                         |
| **Build** | `.\mvnw.cmd clean package` — BUILD SUCCESS, 50 tests, 0 failures |
| **Runtime check** | Performed: MongoDB was up on `localhost:27017` and the `academy` database was already seeded |

## What was built

Ten classes carrying the `//Clase Sn` markers, plus the four edits to existing code. The chain is
entirely hand-written: no resource-server starter, no `JwtDecoder`, no `oauth2ResourceServer` DSL.

### New files

| Marker | File |
| --- | --- |
| S1 | `src/main/java/com/academy/security/User.java` |
| S2 | `src/main/java/com/academy/security/AuthRequest.java` |
| S3 | `src/main/java/com/academy/security/AuthResponse.java` |
| S4 | `src/main/java/com/academy/security/JwtUtil.java` |
| S5 | `src/main/java/com/academy/security/AuthenticationManager.java` |
| S6 | `src/main/java/com/academy/security/SecurityContextRepository.java` |
| S9 | `src/main/java/com/academy/controller/LoginRestController.java` |
| S10 | `src/main/java/com/academy/security/AuthValidator.java` |

### Modified files

| File | Change |
| --- | --- |
| `pom.xml` | `jjwt.version` property = `0.13.0`; `jjwt-api` (compile), `jjwt-impl` + `jjwt-jackson` (runtime). `spring-boot-starter-security` was already declared. |
| `src/main/resources/application.yaml` | `jjwt.secret` with the `${JWT_SECRET:...}` default |
| `config/SecurityConfig.java` | S7 — rewritten chain |
| `repository/IUserRepository.java` | S8 — `Mono<User> findOneByUsername(String)` |
| `service/IUserService.java` | S8 — `searchByUser`, `saveHash` |
| `service/impl/UserServiceImpl.java` | S8 — both implementations + `IRoleRepository` |
| `controller/StudentRestController.java` | S10 — `@PreAuthorize("@authValidator.isValid()")` on `findAll` |

## Per-class notes

**S1 `security.User`** — `@Data @AllArgsConstructor @NoArgsConstructor` implementing `UserDetails`.
Lombok supplies `getUsername`, `getPassword` and `isEnabled` (the field is a primitive `boolean`, so
the generated `isEnabled()` overrides the interface default). `getAuthorities()` maps role names
verbatim with **no `ROLE_` prefix** — this is what makes `hasAuthority('ADMIN')` succeed while
`hasRole('ADMIN')` fails. `password` is `@JsonIgnore`d; it is read once at `/login` and never
serialised. A null `roles` list yields `List.of()` rather than an NPE.

**S4 `JwtUtil`** — jjwt **0.13.0 uses the 0.12-era fluent API**, not the 0.11 `setClaims`/
`parseClaimsJws` style. Verified against the resolved jar with `javap` before writing:
`Jwts.builder().claims(map).subject(..).issuedAt(..).expiration(..).signWith(SecretKey).compact()`
and `Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()`. The secret is
taken as a constructor `@Value` parameter and turned into a `SecretKey` once via
`Keys.hmacShaKeyFor`; the 64-character default is 512 bits, so jjwt selects **HS512** (confirmed in a
live token header: `{"alg":"HS512"}`). Expiration is five hours. One method beyond the spec's four:
`getRolesFromToken`, which narrows the raw `List` from the `roles` claim in one place instead of
leaving an unchecked cast at the call site.

**S5 `AuthenticationManager`** — an invalid token produces `Mono.empty()`, never an error signal.
The exchange then has no security context, `anyExchange().authenticated()` rejects it, and the entry
point writes the 401. `validateToken` swallows `JwtException` and `IllegalArgumentException` and logs
at debug, so a bad signature, a malformed token and an expired token are indistinguishable to the
caller. Token parsing is pure CPU work over a short string, so it stays on the event loop — no
`subscribeOn` needed and no blocking call anywhere in the chain.

**S6 `SecurityContextRepository`** — `BEARER_PREFIX` is compared with `startsWith`, which is
case-sensitive, so a lowercase `bearer ` never matches and falls through to 401 as required.

**S7 `SecurityConfig`** — the `.pathMatchers("/v1/**", "/v2/**").permitAll()` line is gone; only
`/login` is `permitAll` and everything else is `authenticated()`. Spring Security 7.1 lambda DSL
throughout, no `.and()`. The pre-existing `.exceptionHandling(...)` wiring of
`GlobalErrorWebExceptionHandler` as both `authenticationEntryPoint` and `accessDeniedHandler` was
kept — it is what gives 401/403 a JSON body (see the note below). `@EnableReactiveMethodSecurity`
added for S10.

**S8 service additions** — `searchByUser` follows the spec's `zipWhen` shape exactly: the user is
read by username, then each embedded role id is resolved against the `roles` collection so the
name comes from the role document rather than from the embedded snapshot. A null `roles` array
degrades to an empty list instead of throwing, and `status` is unboxed with
`Boolean.TRUE.equals(...)` so a missing `status` field means "not enabled" rather than an NPE.
`saveHash` reuses the existing private `encodePassword` helper, which already runs BCrypt on
`Schedulers.boundedElastic()`.

**S9 `LoginRestController`** — an unknown username makes `searchByUser` emit empty; a wrong password
is removed by `filterWhen`. Both paths land on the same `defaultIfEmpty(401)` with no body, so they
are byte-for-byte identical to the caller. BCrypt verification is deliberately expensive, so
`matches` runs on `Schedulers.boundedElastic()`.

## Deviations from the spec

1. **`SecurityContextRepository.save()` returns `Mono.empty()`, not `null`.** The spec says
   "`save()` returns `null`". Returning a literal `null` from a `Mono<Void>` method is a latent NPE
   the moment anything subscribes to it, and it violates the project's hard rule that every public
   API returns a real `Mono`/`Flux`. `Mono.empty()` expresses the same thing — nothing is stored —
   safely.
2. **`UserServiceImpl` injects `PasswordEncoder`, not `BCryptPasswordEncoder`.** The class already
   had a `PasswordEncoder` field before this change, and `UserServiceImplTest` injects a
   `@Mock PasswordEncoder` through `@InjectMocks`. Narrowing the field type would have broken that
   test, and the acceptance criteria require the existing tests to stay untouched. The bean is the
   same object either way: `SecurityConfig.passwordEncoder()` now **declares** the concrete
   `BCryptPasswordEncoder` return type, so `LoginRestController` gets the concrete type it needs for
   `matches` while every `PasswordEncoder` injection point still resolves to the same bean.
   `IRoleRepository` was added as specified.
3. **`AuthRequest` carries `@NotBlank` on both fields.** Not in the spec, but `CLAUDE.md` mandates
   `@Valid` on request bodies. An empty username now yields 400, not 401. This does not weaken the
   "indistinguishable" property, which is about *existing vs non-existing* users.
4. **`JwtUtil` reads the secret as a constructor `@Value` parameter** rather than an `@Value` field,
   to keep the project's constructor-injection style and let the `SecretKey` be `final`.
5. **`spring-boot-starter-security` was already in `pom.xml`**, so only the three jjwt artifacts were
   added.

## Note on `GlobalErrorWebExceptionHandler`

Its 401/403 branch **does** fire, but not through the `WebExceptionHandler.handle` path the spec's
comment implies. It fires through the `ServerAuthenticationEntryPoint.commence` and
`ServerAccessDeniedHandler.handle` overloads, because `SecurityConfig` wires the same bean into
`.exceptionHandling(...)`. Verified live — both are non-empty JSON bodies. Nothing was changed there.

One wording mismatch worth recording: the spec's contract table says the 401 body carries
`message: "Unauthorized access"`. The handler as written emits
`"Authentication is required to access this resource"` (401) and `"Access to this resource is denied"`
(403) — deliberately generic strings, per its own comment, so the response cannot be used to probe
the security setup. The **shape** matches the contract; the **string** does not. Left as-is, since
`exception/` was explicitly out of scope.

## Acceptance criteria

MongoDB turned out to be running with the `academy` database already seeded (including the password
hashes that `resources/data.json` omits — that file has no `password` field, so a fresh import from
it alone would make every login return 401). That allowed the full list to be checked live against
the packaged jar on `localhost:8080`.

**Verified by the build (`.\mvnw.cmd clean package`, BUILD SUCCESS, 50/50 tests):**

* [x] `./mvnw test` stays green with the existing tests untouched — 50 tests, 0 failures. No test
  file was edited; `UserServiceImplTest` still passes with the extra `IRoleRepository` constructor
  parameter.

**Verified live against a running app + MongoDB:**

* [x] `GET /v1/students` with no header → `401` (was `200` before the change).
* [x] `POST /login` `sysadmin` / `123456789` → `200` with an `access_token` field.
* [x] Wrong password and unknown user both → `401` with an empty body.
* [x] Token decodes to `{"roles":["ADMIN"],"username":"sysadmin","test-value":"sample-test-value",`
  `"sub":"sysadmin","iat":...,"exp":...}` with `exp - iat = 18000` s = exactly five hours;
  header is `{"alg":"HS512"}`.
* [x] `GET /v1/students` with `Authorization: Bearer <token>` → `200` and the student list; without
  it → `401` and the JSON error body.
* [x] Lowercase `bearer <token>` → `401`; the bare token with no prefix → `401`.
* [x] A token with four characters of the signature replaced → `401`.
* [x] `POST /v2/students` — a functional route — → `401` with no change to `RouterConfig`.
* [x] `participant` logs in and its token carries `["USER","INVITED"]` (the seeded document has two
  embedded roles, one more than the spec's table shows) while `sysadmin` carries `["ADMIN"]`, both
  read back from the `roles` collection by the embedded `_id`.
* [x] With `AuthValidator.isValid()` returning `false`, `findAll` → `403` (`"Access to this resource
  is denied"`) while `findById` → `200`. `AuthValidator` was restored to `return true` afterwards.
* [x] `@PreAuthorize("hasRole('ADMIN')")` → `403`; `@PreAuthorize("hasAuthority('ADMIN')")` → `200`,
  confirming the missing `ROLE_` prefix in `getAuthorities()`. The annotation was restored to
  `@authValidator.isValid()` afterwards.

**Not exercised:**

* An expired token was not waited out; expiry rejection rests on `parseSignedClaims` throwing
  `ExpiredJwtException`, which `validateToken` catches alongside every other `JwtException`, and on
  the tampered-token result above.
* `saveHash` has no caller yet — it compiles and follows the same `encodePassword` path as `save`,
  but no request exercises it.
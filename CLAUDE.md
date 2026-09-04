# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Reactive Spring Boot 4.1.1 / Java 25 backend for an academy enrollment domain. The repository is currently a **scaffold**: only `AcademyAppApplication` and the default context-loads test exist. Nearly all work here means creating the first real code for a vertical, so the conventions below are prescriptive, not descriptive.

## Commands

Maven wrapper (`mvnw.cmd` on Windows PowerShell, `./mvnw` under bash):

```powershell
.\mvnw.cmd spring-boot:run                          # run the app (Netty, port 8080)
.\mvnw.cmd clean package                            # build the fat jar
.\mvnw.cmd test                                     # all tests
.\mvnw.cmd test -Dtest=UserServiceImplTest          # single test class
.\mvnw.cmd test -Dtest=UserServiceImplTest#findById_shouldReturnUser   # single test method
```

There is no linter or formatter configured.

Runtime prerequisite: MongoDB on `localhost:27017`, database `academy`. `application.yaml` currently only sets the application name — Mongo connection settings still need to be added, using `${ENV_VAR:default}` placeholders (never hardcoded URLs or secrets).

## Domain model

The entity model is defined by the ER diagram at `src/main/resources/enrollments.png` — **read that image before creating or changing entities**. Collections: `users` (with embedded/referenced `rol`), `rols`, `students`, `courses`, `enrollments` (an enrollment references one student and an array of courses). All ids are Mongo `ObjectId` mapped to Java `String`; most entities carry `status`, `created_at`, `modified_at`.

## Architecture conventions

`.agents/subagents/spring-builder.md` is the authoritative, worked-example spec for building a vertical (model → dto → repository → service → controller). Read it in full before adding a resource. Summary of what it mandates:

**Layers under `com.academy`**: `config/`, `controller/`, `service/` (+ `service/impl/`), `repository/`, `model/`, `dto/`, `exception/`, `util/`.

**Naming**: `MapperConfig`, `UserRestController`, `IUserService` / `UserServiceImpl`, `IUserRepository`, `User`, `UserDto`, `ModelNotFoundException`.

**Generic CRUD spine** — the shape that keeps per-entity code near-empty; add it once, reuse for every entity:
- `IGenericRepository<T, K> extends ReactiveMongoRepository<T, K>` (`@NoRepositoryBean`); each `IXRepository` extends it and stays empty unless a derived query is needed.
- `ICrudService<T, K>` (`findAll`, `findById`, `save`, `update`, `delete`) with an abstract `CrudServiceImpl<T, K>` whose only extension point is `protected abstract IGenericRepository<T, K> getRepository()`. `update` resolves `setId` reflectively, so every model needs a `String` setter for `id` (Lombok `@Data` provides it).

**Controllers**: annotated style at `/v1/<plural>`, DTOs in and out (never entities), return `Mono<ResponseEntity<...>>` / `Mono<ResponseEntity<Flux<...>>>`, `@Valid` on request bodies, 201 `Location` built from the injected `ServerHttpRequest`, and `ModelMapper` injected with an explicit `@Qualifier("defaultMapper")`.

**Lombok**: constructor injection via `@RequiredArgsConstructor` everywhere. Models use `@Data @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode(onlyExplicitlyIncluded = true) @Document(collection = "...")` with `@EqualsAndHashCode.Include` on the `@Id` and `@Field` on the rest. DTOs use `@Data @AllArgsConstructor @NoArgsConstructor @JsonInclude(NON_NULL)` plus jakarta validation annotations.

**Hard rule**: every public API returns `Mono<T>` or `Flux<T>`. No `.block()`, `.toFuture().get()`, or blocking I/O anywhere in a chain.

## Missing dependencies

The conventions above require libraries the POM does not yet declare — add them when first needed:
- `org.modelmapper:modelmapper` (for `MapperConfig` / DTO mapping)
- `spring-boot-starter-validation` (for `@Valid` and jakarta constraints to be enforced)

`spring-boot-starter-security` is on the classpath, so **every endpoint is locked behind HTTP Basic with a generated password until a `SecurityConfig` (`SecurityWebFilterChain`) is written**. Expect 401s on any new endpoint before that exists.

## Reactive reference

`.agents/skills/spring-webflux/SKILL.md` and `advanced.md` hold the project's Reactor cheat-sheet: operator selection (`flatMap` vs `concatMap` vs `switchMap`), `onError*` handling, `WebClient` setup, SSE with `Sinks`, context propagation, and testing with `WebTestClient` + `StepVerifier`. Prefer those patterns for tests: `WebTestClient` for controllers, `StepVerifier` for services.

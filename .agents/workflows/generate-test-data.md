# Generate Test Data

Seed the `Student`, `Course`, and `Enrollment` catalogs in this reactive Spring Boot backend by generating JSON payloads and posting them to the running API.

## Assigned Agent

Use the `spring-test-data-generator` agent to produce `student.json`, `course.json` and `enrollment.json` in `src/main/resources/test-data/` path.

This workflow handles delegation, execution, and reporting.

## Required Input

* Base URL (default `http://localhost:8080`).

## Prerequisites

* The application is running with MongoDB reachable.

## Workflow

1. Delegate to `spring-test-data-generator` to produce the JSON files.
2. For each resource, POST every record to `{baseUrl}/v1/<resource-path>`
   (`/v1/roles`, `/v1/users`, `/v1/students`, `/v1/courses`, `/v1/enrollments`) with `Content-Type: application/json`.
3. Verify each response is `201 Created` and capture the returned `id`. Stop and report on any failure — for a `400`, ask the agent to re-check the payload against the DTO it generated from.
4. Report: records inserted per resource, all created ids, and anything skipped (with reason).

## Acceptance Criteria

- Only `Student`, `Course` and `Enrollment` are produce json files. `Role` and `User` are existing.
- No `id` is sent on create.
- No audit fields (`createdAt`, `updatedAt`) are sent on create.
- Final report lists every created id.

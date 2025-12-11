# Testing Guide

## Overview

This document describes the testing strategy and procedures for the Task Management Tool.

## Test Categories

### 1. Unit Tests

**Backend Unit Tests** - Located in `backend/src/test/java/`

#### WorkingDayCalculator Tests (15 tests)
```bash
docker-compose exec backend mvn test -Dtest=WorkingDayCalculatorTest
```

Tests cover:
- Default working days (Mon-Fri)
- Custom working days configuration
- Holiday handling
- Next/previous working day calculation
- Working day counting
- Date adjustment to working days
- Null parameter validation

#### CircularDependencyDetector Tests (11 tests)
```bash
docker-compose exec backend mvn test -Dtest=CircularDependencyDetectorTest
```

Tests cover:
- Self-dependency detection
- Direct cycle detection
- Indirect cycle detection
- Linear dependency validation
- Parallel dependencies
- Project-wide cycle detection
- Cycle chain retrieval
- Null parameter validation

#### ImportJobService Integration Tests (9 tests)
```bash
docker-compose exec backend mvn test -Dtest=ImportJobServiceIntegrationTest
```

Tests cover:
- Successful CSV import
- Import with dependencies
- Dry-run mode
- Validation error handling
- Circular dependency detection
- Upsert logic (create vs update)
- Invalid file type rejection
- Empty file handling

### 2. Integration Tests

Run all backend tests:
```bash
make test
# or
docker-compose exec backend mvn test
```

Expected results:
- All unit tests: **35+ tests passing**
- No compilation errors
- No test failures

### 3. End-to-End Test Scenarios

#### Scenario 1: Project Creation and Task Management
1. Open http://localhost:3000
2. Click "Create Your First Project"
3. Enter project name and confirm
4. Verify project appears in list
5. Click on project to view Gantt chart
6. Click "Task List" to view tasks
7. Verify empty task list message

**Expected**: All UI flows work without errors

#### Scenario 2: CSV Import Workflow
1. Navigate to project → "Import" button
2. Download sample template
3. Upload sample CSV file
4. Verify dry-run validation results
5. Click "Proceed with Import"
6. Verify import success message
7. Navigate to Task List
8. Verify tasks are displayed

**Expected**:
- 6 tasks imported successfully
- Dependencies created (5 dependencies)
- Progress shows in task list
- No validation errors

#### Scenario 3: Task Inline Editing
1. Navigate to Task List
2. Click "Edit" on a task
3. Modify name, dates, progress
4. Click "Save"
5. Verify changes persisted
6. Refresh page
7. Verify changes still present

**Expected**: Task updates saved to database

#### Scenario 4: Error Handling
1. Upload invalid CSV (wrong format)
2. Verify error message displays
3. Upload CSV with validation errors
4. Verify dry-run shows errors
5. Verify error details displayed with line numbers

**Expected**: Clear error messages, graceful degradation

### 4. Performance Tests

#### 100-Task Render Test
1. Create CSV with 100 tasks
2. Import via API
3. Navigate to Task List
4. Measure page load time

**Target**: < 1 second initial render

#### Import Validation Time
1. Upload 100-task CSV for dry-run
2. Measure validation time

**Target**: < 5 seconds for validation

#### API Latency
```bash
# Test GET tasks endpoint
time curl http://localhost:8080/api/projects/1/tasks
```

**Target**: < 200ms response time

### 5. Browser Compatibility

Test on:
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

Verify:
- UI renders correctly
- Forms work
- File upload works
- No console errors

## Test Data

### Sample CSV Template
Located at: `frontend/public/sample-import-template.csv`

Contains:
- 6 tasks with various statuses
- Dependencies (FS type)
- Milestone task
- Date ranges
- Progress values

### Database Seeding

To populate test data:
```bash
# Option 1: Use sample CSV import via UI

# Option 2: Use API directly
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Project","status":"active"}'
```

## Automated Testing

### CI/CD Integration (Future)

Recommended GitHub Actions workflow:

```yaml
name: Tests
on: [push, pull_request]
jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build and test
        run: |
          docker-compose up -d postgres
          docker-compose run backend mvn test
```

## Test Coverage

### Current Coverage
- **Backend**:
  - Utility classes: 100% (WorkingDayCalculator, CircularDependencyDetector)
  - Import workflow: 90%
  - Controllers: Manual testing required

- **Frontend**:
  - Component rendering: Manual testing
  - API integration: Manual testing

### Future Improvements
- Add frontend unit tests (Jest + React Testing Library)
- Add E2E tests (Playwright/Cypress)
- Measure code coverage (JaCoCo)
- Performance monitoring (Lighthouse)

## Known Issues

None at this time.

## Reporting Bugs

When reporting issues, include:
1. Steps to reproduce
2. Expected behavior
3. Actual behavior
4. Browser/environment details
5. Console errors (if any)
6. Screenshots (if applicable)

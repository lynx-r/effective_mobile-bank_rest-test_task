# Authorization Server Tests TODO

## Controller Tests
- [ ] Create RegistrationControllerTest
  - [ ] Test successful registration endpoint
  - [ ] Test validation errors (invalid request body)
  - [ ] Test service exceptions handling

## Service Tests
- [x] Create UserServiceImplTest
  - [x] Test successful user registration
  - [x] Test registration with existing username
  - [x] Test registration with existing email
  - [x] Test registration when default role not found
  - [x] Test password encoding
  - [x] Test Kafka event publishing

## Integration Tests
- [ ] Consider adding integration tests if needed

## Test Infrastructure
- [ ] Ensure proper test dependencies in build.gradle
- [ ] Set up test configuration if needed

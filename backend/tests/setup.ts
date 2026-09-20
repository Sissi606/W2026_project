/**
 * src/config/env.ts validates its inputs at import time and throws when a
 * required variable is absent, so the suite supplies fixed values before any
 * module under test is loaded. These deliberately do not read from .env: tests
 * must produce the same result on every machine.
 */
process.env.JWT_SECRET = 'test-jwt-secret-value-for-unit-tests';
process.env.GOOGLE_CLIENT_ID = 'test-client-id.apps.googleusercontent.com';
process.env.DEVELOPER_FIRST_NAME = 'Ada';
process.env.DEVELOPER_LAST_NAME = 'Lovelace';
process.env.SERVER_PUBLIC_IP = '';
process.env.NODE_ENV = 'test';

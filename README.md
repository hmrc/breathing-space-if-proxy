# Breathing Space Proxy

Allows users to create and manage Breathing Space periods and retrieve related individual information.

## Requirements

This service is written in [Scala 3.x](http://www.scala-lang.org/) and [Play 3.x](http://playframework.com/), so needs at least a [JRE 21](http://www.oracle.com/technetwork/java/javase/downloads/index.html) to run.

## API

| *Task*                                                                                         | *Supported Methods* | *Description*                                                                                                                            | Status |
|------------------------------------------------------------------------------------------------|----------------------|------------------------------------------------------------------------------------------------------------------------------------------|--------|
| ```/individuals/breathing-space/{nino}/{periodId}/coding-out-debts```                         | GET                  | Retrieves coded-out debts for a person in Breathing Space.                                                                               | Live   |
| ```/individuals/breathing-space/{nino}/{periodId}/debts```                                     | GET                  | Retrieves HMRC debts for a person in Breathing Space.                                                                                     | Live   |
| ```/individuals/breathing-space/{nino}/details```                                              | GET                  | Retrieves personal information for a person in Breathing Space.                                                                           | Live   |
| ```/individuals/breathing-space/{nino}/periods```                                              | GET                  | Retrieves all Breathing Space periods for a Nino.                                                                                         | Live   |
| ```/individuals/breathing-space/{nino}/periods```                                              | PUT                  | Updates all Breathing Space periods for a Nino.                                                                                           | Live   |
| ```/individuals/breathing-space/periods```                                                     | POST                 | Creates new Breathing Space periods for a given Nino.                                                                                     | Live   |
| ```/individuals/breathing-space/{nino}/memorandum```                                                     | GET                 | Retrieves Memorandum for a given Nini.                                                                                     | Live   |

## Configuration

All the microservices used by Breathing Space Proxy require host and port settings. The integration framework service also requires an environment, auth token, and context.

| *Key*                                         | *Description*                                      |
|-----------------------------------------------|----------------------------------------------------|
| `microservice.services.auth.host`             | The host of the auth service                      |
| `microservice.services.auth.port`             | The port of the auth service                      |
| `microservice.services.integration-framework.host` | The host of the integration framework service |
| `microservice.services.integration-framework.port` | The port of the integration framework service |
| `microservice.services.integration-framework.environment` | The integration framework environment |
| `microservice.services.integration-framework.auth-token` | The auth token used by the integration framework |
| `microservice.services.integration-framework.context` | The integration framework context |
| `microservice.services.fandf.protocol`        | The protocol used by the fandf service            |
| `microservice.services.fandf.host`            | The host of the fandf service                    |
| `microservice.services.fandf.port`            | The port of the fandf service                    |
| `feature.flag.memorandum.enabled`             | Enables the memorandum feature                   |
| `ninoHashingKey`             | Nino HashingKey                   |

## How to test the project

### Unit Tests

- **Unit test the entire test suite:** `sbt test`
- **Unit test a single spec file:** `sbt "Test/testOnly *fileName"` (for example: `sbt "Test/testOnly *IndividualDetailsControllerSpec"`)

### Integration tests

- **Run integration tests:** `sbt it/test`

### Running the service

Start a terminal and enter:
```
$ sm --start AUTH AUTH_LOGIN_API AUTH_LOGIN_STUB USER_DETAILS IDENTITY_VERIFICATION ASSETS_FRONTEND -f
$ sbt run
```

!!**IMPORTANT**!!
Before committing, always run from the console: `./precheck.sh`
`sbt-bobby` should be installed in your local .sbt folder for this to work.

### Debugging the service
```
$ sbt -jvm-debug 5005
```

- Then create a remote JVM debug run config targeting this port

## Reporting Issues

Report issues at [Breathing Space issues](https://github.com/hmrc/breathing-space-if-proxy/issues).

## License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

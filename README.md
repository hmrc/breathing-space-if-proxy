
# Breathing Space Proxy

"***Breathing Space** is a 2017 government's manifesto commitment to help people with problem debt to better control their finances.  
It will pause enforcement actions; freeze interest, fees, and charges, and pause creditor contact with debtors where it relates to debt repayment for a 60-day period. These protections will be accessible via professional debt advice. During this period, individuals will receive professional debt advice to find a long-term solution to their financial difficulties.*"

This &#xb5;service enables interaction between PEGA Cloud, the platform controlling the Breathing Space workflow, and HMRC's Strategic Systems via a REST API fully documented on the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/breathing-space-if-proxy/1.0).

#### &nbsp; &nbsp; PEGA Cloud &#8594; MDTP &#8594; Integration-Framework

### Prerequisites
- Scala 2.12.12
- Java 8
- sbt +1.3

### Running the service

Start a terminal and enter:
```
$ sm --start AUTH
$ sbt run
```

!!**IMPORTANT**!!
Before committing, always run from the console: `./precheck.sh`

### Highlighted SBT Tasks

Task | Description | Command
:-------|:------------|:-----
test | Runs the standard unit tests | ```$ sbt test```
it:test  | Runs the integration tests | ```$ sbt it:test ```
dependencyUpdates |  Shows a list of project dependencies that can be updated | ```$ sbt dependencyUpdates```
dependencyUpdatesReport | Writes a list of project dependencies to a file | ```$ sbt dependencyUpdatesReport```

### Reporting Issues

Report issues at [BS issues](https://github.com/hmrc/breathing-space-if-proxy/issues).

### License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

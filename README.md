# pagopa-accounting-reconciliation-bdi-ingestion

## Overview

The application is designed to ingest the BDI (Banca d'Italia) data for the Accounting Reconciliation project.

## Technology Stack

- Kotlin
- Spring Boot

---

## Start Project Locally 🚀

### Prerequisites

- docker

### Populate the environment

The microservice needs a valid `.env` file in order to be run.

If you want to start the application without too much hassle, you can just copy `.env.local.example` to get a good default configuration using the following command.

```shell
cp .env.local.example .env
```



If you want to customize the application environment, reference this table:
| Variable name                                         | Description                                                                                                                                                | type              | default |
|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|---------|
| ROOT_LOGGING_LEVEL                                    | Application root logger level                                                                                                                              | string            | INFO    |
| APP_LOGGING_LEVEL                                     | it.pagopa logger level                                                                                                                                     | string            | INFO    |
| WEB_LOGGING_LEVEL                                     | Web logger level                                                                                                                                           | string            | INFO    |
| NETTY_TCP_SSL_PROVIDER_LOGGING_LEVEL                  | Netty SSL provider logger level                                                                                                                            | string            | DEBUG   |
| NETTY_HANDLER_SSL_LOGGING_LEVEL                       | Netty SSL handler logger level                                                                                                                             | string            | DEBUG   |
| BDI_SERVER_URI                                        | BDI server uri                                                                                                                                             | string            |         |
| BDI_SERVER_READ_TIMEOUT_MILLIS                        | Read timeout in milliseconds for the bdi server                                                                                                            | integer           |         |
| BDI_SERVER_CONNECTION_TIMEOUT_MILLIS                  | Connection timeout in milliseconds for the bdi server                                                                                                      | integer           |         |
| ACCOUNTING_DATA_INGESTION_JOB_EXECUTION_CRON          | Accounting data ingestion job execution cron expression                                                                                                    | string            |         |
| ACCOUNTING_DATA_INGESTION_JOB_RETRIES                 | Accounting data ingestion job number of retries                                                                                                            | integer           |         |
| ACCOUNTING_DATA_INGESTION_JOB_MIN_BACKOFF_SECONDS     | Accounting data ingestion job min backoff in seconds                                                                                                       | integer           |         |
| ACCOUNTING_DATA_INGESTION_JOB_UNZIP_CONCURRENCY       | Accounting data ingestion job unzip service parallelism                                                                                                    | integer           |         |
| ACCOUNTING_DATA_INGESTION_JOB_UNZIP_FILES_BUFFER_SIZE | Accounting data ingestion job saved files buffer size                                                                                                      | integer           |         |
| ACCOUNTING_DATA_INGESTION_JOB_PARSING_CONCURRENCY     | Accounting data ingestion job parsing service parallelism                                                                                                  | integer           |         |
| MATCHING_JOB_EXECUTION_CRON                           | Scheduled matching service cron expression                                                                                                                 | string            |         |
| MATCHING_JOB_EXECUTION_RETRIES                        | Scheduled matching service number of retry                                                                                                                 | integer           |         |
| MATCHING_JOB_EXECUTION_MIN_BACKOFF_SECONDS            | Scheduled matching service minimum backoff time                                                                                                            | integer           |         |
| MATCHING_JOB_TIMEOUT                                  | Scheduled matching service query timeout                                                                                                                   | integer           |         |
| MATCHING_JOB_BDI_TIMESHIFT                            | Scheduled matching service timeshift for the BDI query                                                                                                     | string            |         |
| MATCHING_JOB_FDR_TIMESHIFT                            | Scheduled matching service timeshift for FDR query                                                                                                         | string            |         |      
| MATCHING_JOB_QUERY_CAUSALE_REGEX                      | Scheduled matching service regex used for extract the END2END_ID from CAUSALE                                                                              | string            |         |
| MATCHING_JOB_DATABASE                                 | Scheduled matching service database used                                                                                                                   | string            |         |
| MATCHING_JOB_DATABASE_TABLE_BDI                       | Scheduled matching service BDI table name                                                                                                                  | string            |         |
| MATCHING_JOB_DATABASE_TABLE_FDR                       | Scheduled matching service FDR table name                                                                                                                  | string            |         |
| MATCHING_JOB_DATABASE_TABLE_MATCHING                  | Scheduled matching service MATCHING table name                                                                                                             | string            |         |
| MATCHING_JOB_MATHING_TIMESHIFT                        | Scheduled matching service timeshift for the MATCHING table query                                                                                          | string            |         |
| BDI_CERTIFICATE_PATH                                  | Path to the BDI pem certificate                                                                                                                            | string            |         |
| AZURE_DATA_EXPLORER_DOMAIN                            | Data Explorer domain                                                                                                                                       | string            |         |
| AZURE_DATA_EXPLORER_DATABASE                          | Data Explorer database name                                                                                                                                | string            |         |
| AZURE_DATA_EXPLORER_DATABASE_TABLE                    | Data Explorer table name                                                                                                                                   | string            |         |
| MONGO_HOST                                            | Host where MongoDB instance used to persist wallet data                                                                                                    | hostname (string) |         |
| MONGO_PORT                                            | Port where MongoDB is bound to in MongoDB host                                                                                                             | number            |         |
| MONGO_USERNAME                                        | MongoDB username used to connect to the database                                                                                                           | string            |         |
| MONGO_PASSWORD                                        | MongoDB password used to connect to the database                                                                                                           | string            |         |
| MONGO_SSL_ENABLED                                     | Whether SSL is enabled while connecting to MongoDB                                                                                                         | string            |         |
| MONGO_DB_NAME                                         | Mongo database name                                                                                                                                        | string            |         |
| MONGO_MIN_POOL_SIZE                                   | Min amount of connections to be retained into connection pool. See docs *                                                                                  | string            |         |
| MONGO_MAX_POOL_SIZE                                   | Max amount of connections to be retained into connection pool.See docs *                                                                                   | string            |         |
| MONGO_MAX_IDLE_TIMEOUT_MS                             | Max timeout after which an idle connection is killed in milliseconds. See docs *                                                                           | string            |         |
| MONGO_CONNECTION_TIMEOUT_MS                           | Max time to wait for a connection to be opened. See docs *                                                                                                 | string            |         |
| MONGO_SOCKET_TIMEOUT_MS                               | Max time to wait for a command send or receive before timing out. See docs *                                                                               | string            |         |
| MONGO_SERVER_SELECTION_TIMEOUT_MS                     | Max time to wait for a server to be selected while performing a communication with Mongo in milliseconds. See docs *                                       | string            |         |
| MONGO_WAITING_QUEUE_MS                                | Max time a thread has to wait for a connection to be available in milliseconds. See docs *                                                                 | string            |         |
| MONGO_HEARTBEAT_FREQUENCY_MS                          | Hearth beat frequency in milliseconds. This is an hello command that is sent periodically on each active connection to perform an health check. See docs * | string            |         |
| MONGO_REPLICA_SET_OPTION                              | Additional replica set options for the MongoDB connection string                                                                                           | string            |         |

\* For Mongo connection string options
see [docs](https://www.mongodb.com/docs/drivers/java/sync/v4.3/fundamentals/connection/connection-options/#connection-options)


### Run docker container

```shell
docker compose up --build
```

### MongoDB

You can access the MongoDB database using either [Mongo Express](https://github.com/mongo-express/mongo-express) or
[MongoDB Compass](https://www.mongodb.com/products/tools/compass)

#### Mongo Express

Go to `http://localhost:8201` and use the following credentials:
- Username: `admin`
- Password: `pass`

---

## Develop Locally 💻

### Prerequisites

- git
- gradle
- jdk-21

### Run the project

Before locally running the application you need to export the environment variables contained in your `.env` file using the following command:
```shell
set -a; source .env; set +a
```

To run the application use:
```shell
./gradlew bootRun
```


### Testing 🧪

#### Unit testing

To run the **Junit** tests:

```shell
./gradlew test
```

#### Mocks of external services
For all the information regarding the used mocks go under the _./docker_ directory.

For the generation of the testing data used for the BDI mock, see the README file under _./docker/bdi_mock/data/_.

### Dependency management 🔧

To support reproducible build this project has the following gradle feature enabled:

- [dependency lock](https://docs.gradle.org/8.1/userguide/dependency_locking.html)
- [dependency verification](https://docs.gradle.org/8.1/userguide/dependency_verification.html)

#### Dependency lock

This feature use the content of `gradle.lockfile` to check the declared dependencies against the locked one.

If a transitive dependencies have been upgraded the build will fail because of the locked version mismatch.

The following command can be used to upgrade dependency lockfile:

```shell
./gradlew dependencies --write-locks 
```

Running the above command will cause the `gradle.lockfile` to be updated against the current project dependency
configuration

#### Dependency verification

This feature is enabled by adding the gradle `./gradle/verification-metadata.xml` configuration file.

Perform checksum comparison against dependency artifact (jar files, zip, ...) and metadata (pom.xml, gradle module
metadata, ...) used during build
and the ones stored into `verification-metadata.xml` file raising error during build in case of mismatch.

The following command can be used to recalculate dependency checksum:

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

In the above command the `clean`, `spotlessApply` `build` tasks where chosen to be run
in order to discover all transitive dependencies used during build and also the ones used during
spotless apply task used to format source code.

The above command will upgrade the `verification-metadata.xml` adding all the newly discovered dependencies' checksum.
Those checksum should be checked against a trusted source to check for corrispondence with the library author published
checksum.

`/gradlew --write-verification-metadata sha256` command appends all new dependencies to the verification files but does
not remove
entries for unused dependencies.

This can make this file grow every time a dependency is upgraded.

To detect and remove old dependencies make the following steps:

1. Delete, if present, the `gradle/verification-metadata.dryrun.xml`
2. Run the gradle write-verification-metadata in dry-mode (this will generate a verification-metadata-dryrun.xml file
   leaving untouched the original verification file)
3. Compare the verification-metadata file and the verification-metadata.dryrun one checking for differences and removing
   old unused dependencies

The 1-2 steps can be performed with the following commands

```Shell
rm -f ./gradle/verification-metadata.dryrun.xml 
./gradlew --write-verification-metadata sha256 clean spotlessApply build --dry-run
```

The resulting `verification-metadata.xml` modifications must be reviewed carefully checking the generated
dependencies checksum against official websites or other secure sources.

If a dependency is not discovered during the above command execution it will lead to build errors.

You can add those dependencies manually by modifying the `verification-metadata.xml`
file adding the following component:

```xml

<verification-metadata>
    <!-- other configurations... -->
    <components>
        <!-- other components -->
        <component group="GROUP_ID" name="ARTIFACT_ID" version="VERSION">
            <artifact name="artifact-full-name.jar">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
            <artifact name="artifact-pom-file.pom">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
        </component>
    </components>
</verification-metadata>
```

Add those components at the end of the components list and then run the

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

that will reorder the file with the added dependencies checksum in the expected order.

Finally, you can add new dependencies both to gradle.lockfile writing verification metadata running

```shell
 ./gradlew dependencies --write-locks --write-verification-metadata sha256 --no-build-cache --refresh-dependencies
```

For more information read the
following [article](https://docs.gradle.org/8.1/userguide/dependency_verification.html#sec:checksum-verification)

## Contributors 👥

Made with ❤️ by PagoPA S.p.A.

### Maintainers

See `CODEOWNERS` file
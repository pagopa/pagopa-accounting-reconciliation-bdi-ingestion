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


| Variable name                        | Description                                           | type    | default |
|--------------------------------------|-------------------------------------------------------|---------|---------|
| ROOT_LOGGING_LEVEL                   | Application root logger level                         | string  | INFO    |
| APP_LOGGING_LEVEL                    | it.pagopa logger level                                | string  | INFO    |
| WEB_LOGGING_LEVEL                    | Web logger level                                      | string  | INFO    |
| BDI_SERVER_URI                       | BDI server uri                                        | string  |         |
| BDI_SERVER_READ_TIMEOUT_MILLIS       | Read timeout in milliseconds for the bdi server       | integer |         |
| BDI_SERVER_CONNECTION_TIMEOUT_MILLIS | Connection timeout in milliseconds for the bdi server | integer |         |
| DATA_INGESTION_JOB_EXECUTION_CRON    | Data ingestion job execution cron expression          | string  |         |
| CERTIFICATE_FILE_NAME                | Name of the pem certificate                           | string  |         |


### Run docker container

```shell
docker compose up --build
```

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
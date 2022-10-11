# Files Upload Application for Buckets in Oracle Cloud Infrastructure

<!-- TOC -->
* [About](#about)
  * [What you will need](#what-you-will-need)
    * [Service](#service)
  * [System Requirements](#system-requirements)
  * [Features](#features)
    * [On-demand and service compatible](#on-demand-and-service-compatible)
    * [Multiple directories](#multiple-directories)
    * [Job scheduling](#job-scheduling)
    * [Attempts failure](#attemps-failure)
  * [Coming soon features](#coming-soon-features)
  * [Settings](#settings)
    * [application.properties](#applicationproperties)
  * [About OCI API key - .oci](#about-oci-api-key---oci)
  * [Sample **.oci** file:](#sample-oci-file-)
  * [Sample application.properties file](#sample-applicationproperties-file)
  * [Building](#building)
    * [Running DEV profile](#running-dev-profile)
  * [Running](#running)
    * [On-demand](#on-demand)
  * [Code Of Conduct](#code-of-conduct)
  * [Contributing](#contributing)
  * [License](#license)
<!-- TOC -->

# About

The upload-bucket-service project is an automatic file¹ upload application for Buckets in the Oracle Cloud Infrastructure Object Storage (OCI) service.

With few configurations it is possible to map multiple directories² and upload to Oracle Object Storage, allowing you to configure file overwriting and scheduling.

You will be able to:

- Upload files from multiple directories;
- Assist in the migration of extensive data;
- Save and replicate backup files in a secure and automated way.

**Caution:** the application does not sync deleted files, that is, it does not delete the file in the Bucket, as the intention is to provide a secure way of syncing files, especially for backups.
For file updates, it will only update modified files if the `service.folders[*].overwriteExistingFile` parameter is `true`.

**Note 1:** *file size limit is 50GB.*

**Note 2:** *when used for the purpose of transferring backups, you must remove the OBJECT_READ permission in the OCI permissions policy and disable file overwriting for the directory. It is recommended to disable the OBJECT_DELETE permission, also in the OCI permissions policy for the user in which it is used to communicate with the service's API, thus ensuring greater security and integrity of the backup files. [More info see Securing Object Storage](https://docs.oracle.com/en-us/iaas/Content/Security/Reference/objectstorage_security.htm)*

## What you will need

- OCI Client API key file and key file - see more details on [About OCI API key - .oci](#about-oci-api-key---oci);
- Configure directories in application.properties file;
- Only that!

See more details on [Settings](#settings) below.

### Service

See [On-demand and service compatible](#on-demand-and-service-compatible) below.

## System Requirements

- Java: Java 1.8, Java 11 or later;
- Operational System: Windows, Unix/Linux, MacOS (not tested);
- RAM: ~5MB (may vary when file upload is running);
- Disk space: ~35MB.

## Features

### On-demand and service compatible

The application can be run on demand or configured as a service on Windows and Linux.

- [Install as Unix/Linux Service;](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment.installing.nix-services)
- [Install as Microsoft Windows Service.](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment.installing.windows-services)

### Multiple directories

The application allows the configuration of one or more directories, in which it is possible to enable or disable it and enable the overwriting of files.
Directory analysis is performed in parallel by multiple threads and asynchronously, thus ensuring better performance and without spending resources for a long time.

### Job scheduling

The application uses Cron expression notation, in which it is possible to specify an interval or time for the execution of the job. You can set a default value for all jobs (`service.cron`) or set specific values for each folder (`service.folders[*].cron`).

Samples:

| Expression     | Meaning                    |
|----------------|----------------------------|
| 0 0 12 * * ?   | Execute at 12:00 every day |
| 0 15 10 ? * *  | Execute at 10:15 every day |
| 0/10 * * * * ? | Execute every 10 seconds   |
| 0 0/30 * * * ? | Execute every 30 minutes   |

### Attempts failure

Allows you to set execution attempts when a failure occurs during job processing.

## Coming soon features

Some features that are being planned to be added:

- Sending notification of the job result by e-mail;
- Support for sending to multiple buckets.
  
If you have a suggestion, feel free to open a new issue.

## Settings

Installation folder structure:

```
root
│   upload-bucket-service.jar
│   .oci
|   application.properties
```

### application.properties

**Attention:** You must have at least one directory using the global `service.cron` or you will get the exception `No steps were created for the job`.

| Property                                 | Description                                                     | Required | Default Value             | Type    |
|------------------------------------------|-----------------------------------------------------------------|----------|---------------------------|---------|
| service.cron                             | Default cron expression to all jobs execution                   | No       | "0/10 * * * * ?"          | String  |
| service.attemptsFailure                  | Number of attempts when a failure occurs                        | No       | 1                         | int     |
| service.oci.profile                      | Profile session of .oci configuration                           | Yes      | "DEFAULT"                 | String  |
| service.oci.bucket                       | OCI Bucket name                                                 | Yes      |                           | String  |
| service.folders[*]                       | Folders configuration                                           | Yes      |                           | List    |
| service.folders[*].directory             | Folder path (need to include escape character for \ on Windows) | Yes      |                           | String  |
| service.folders[*].cron                  | Cron expression specifies to the folder                         | No       | Value from *service.cron* | String  |
| service.folders[*].overwriteExistingFile | Enable/disable file overwriting                                 | No       | false                     | boolean |
| service.folders[*].enabled               | Enables/disables folder processing.                             | No       | true                      | boolean |
| service.folders[*].mapToBucketDir        | Set the directory to use in bucket. Leave empty to use root.    | No       |                           | String  |

##  About OCI API key - .oci 

- [To create a Customer Secret key;](https://docs.oracle.com/en-us/iaas/Content/Identity/Tasks/managingcredentials.htm#create-secret-key)
- [To get the config file snippet for an API signing key.](https://docs.oracle.com/en-us/iaas/Content/Identity/Tasks/managingcredentials.htm#)

After getting the file, rename the file to **.oci** and paste it in the same folder as the application. 

## Sample **.oci** file:

```properties
[DEFAULT]
user=
fingerprint=
tenancy=
region=
key_file=
```

## Sample application.properties file

```properties
service.cron=0 0/30 * * * ?
service.oci.profile=DEFAULT
service.oci.bucket=bkp_bd
service.folders[0].directory=C:/temp
service.folders[0].cron=0 0/10 * * * ?
service.folders[1].directory=C:/temp2
service.folders[1].overwriteExistingFile=false
service.folders[1].enabled=true
service.folders[2].directory=C:/temp3
service.folders[2].overwriteExistingFile=false
service.folders[2].enabled=true
service.folders[3].directory=C:/temp4
service.folders[3].overwriteExistingFile=false
service.folders[3].enabled=true
service.folders[4].directory=C:/temp4
service.folders[4].overwriteExistingFile=false
service.folders[4].enabled=true
service.attemptsFailure=5
```

## Building

This project was written in Java with Spring Batch and Oracle Cloud Infrastructure Client SDK.

To build we use Maven.

### Running DEV profile

The DEV profile should be used for debugging and testing. You will need to change the value of the `--oci` property.

Go to the project root directory and run:

```bash
mvn clean package install
mvn spring-boot:run -D"spring-boot.run.profiles=dev" -D"spring-boot.run.arguments"="--oci=/path/to/.oci"
```

The project has configuration files for IntelliJ IDEA, allowing these configurations to be performed in the IDE:

- Maven
  - [clean, install]
  - [clean, package]
- Spring Boot
  - upload-bucket-service (DEV)

## Running

### On-demand

```bash
java -jar upload-bucket-service.jar
```

## Code Of Conduct

We encourage community participation, but be aware of the rules in our Code of Conduct.

See [CODE OF CONDUCT](/CODE_OF_CONDUCT.md) for details.

## Contributing

upload-bucket-service is an open source project, stay free to participate with us. 

See [CONTRIBUTING](/CONTRIBUTING.md) for details.

## License

Copyright (c) 2022, Alex Maycon da Silva, [https://www.alexmaycon.dev](https://www.alexmaycon.dev). All rights reserved. Licensed under the Apache License, Version 2.0 (the "License").

See [LICENSE](/LICENSE.md) for more details.
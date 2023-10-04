# File Upload Application for Buckets in Oracle Cloud Infrastructure

[![Doar](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/donate/?hosted_button_id=LZ67TDQWYGKTG)

<!-- TOC -->
* [README - Português-BR](/README.pt-br.md)
* [About](#about)
  * [What you will need](#what-you-will-need)
    * [Service](#service)
  * [System Requirements](#system-requirements)
  * [Features](#features)
    * [On-demand and service compatible](#on-demand-and-service-compatible)
    * [Multiple directories](#multiple-directories)
    * [Multiple buckets](#multiple-buckets)
    * [Job scheduling](#job-scheduling)
    * [Attempts failure](#attempts-failure)
    * [ZIP compression and encryption](#zip-compression-and-encryption)
	* [Generation of Pre-authenticated Requests](#generation-of-pre-authenticated-requests)
    * [Webhook Notification](#webhook-notification)
	* [Sending email with Sendgrid](#sending-email-with-sendgrid)
  * [Settings](#settings)
    * [application.properties](#applicationproperties)
  * [About OCI API key (.oci)](#about-oci-api-key-oci)
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

The upload-bucket-service project is an automatic file¹ upload application for Buckets in the Oracle Cloud Infrastructure (OCI) Object Storage service.

With few configurations it is possible to map multiple directories² and upload to Oracle Object Storage, allowing you to configure file overwriting, scheduling and webhook notification.

You will be able to:

- Upload files from multiple directories;
- Assist in the migration of extensive data;
- Save and replicate backup files in a secure and automated way.
- Configure webhooks to receive notification of each run with job status and details.

See all samples on [Wiki.](https://github.com/alexmaycon/upload-bucket-service/wiki)

**Caution:** this app does not sync deleted files, that is, it does not delete the file in the Bucket, as the intention is to provide a secure way of syncing files, especially for backups.
For file updates, it will only update modified files if the `service.folders[*].overwriteExistingFile` parameter is `true`.

**Note 1:** *file size limit is 50GB.*

**Note 2:** *when used for the purpose of transferring backups, you must remove the OBJECT_READ permission in the OCI permissions policy and disable file overwriting for the directory. It is recommended to disable the OBJECT_DELETE permission, also in the OCI permissions policy for the user in which it is used to communicate with the service's API, thus ensuring greater security and integrity of the backup files. [More info see Securing Object Storage](https://docs.oracle.com/en-us/iaas/Content/Security/Reference/objectstorage_security.htm)*

## What you will need

- OCI Client API key file and key file - see more details on [About OCI API key (.oci)](#about-oci-api-key-oci);
- Configure directories in application.properties file;
- **Only that!**

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

### Multiple buckets

You can choose to send to the global bucket or to a different bucket per directory.

- `service.oci.bucket` for global configuration;
- `service.folders[*].oci.bucket` for specific folder.

If the bucket does not exist, use the 'createBucketIfNotExists' option to create a new bucket.

- `service.oci.createBucketIfNotExists` for global configuration;
- `service.folders[*].oci.createBucketIfNotExists=true` for a specific folder.

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

### Compression for ZIP and encryption

Allows every file to be compressed to a ZIP file and encrypted for greater security.

To enable, simply add to your `application.properties` file:

```
service.zip.enabled= true
```

If you want to add ZIP encryption using a password:

```
service.zip.password=Yourpa55w0rd
```

Or you can define the environment variable `UBS_ZIP_PWD` - it will always have priority when both locations have the password defined.

```bash
export UBS_ZIP_PWD=Yourpa55w0rd
```

### Generation of Pre-authenticated Requests

Pre-authenticated requests provide a way to allow users to access a bucket or object without having their own credentials through a URL generated at file upload time.

Requests are created with read permission for the sent file and do not allow writing or listing of other files in the bucket.

**The expiration date of the created request is 6 months.**

To enable, simply add to your `application.properties` file:

```
service.oci.generatePreauthenticatedUrl=true
```

When enabled, this URL is sent in the webhook notification and email.

### Webhook notification

You can configure an API URL that you will be **notified** when the job finishes running (either to failure or success). **Your API must accept the POST method.**

The body can be JSON or XML, just configure the `service.hookContentType` with one of the values:

- `application/json`
- `application/xml`

**Sample JSON success:**

On `details` field, each directory is separated by the '¢' character and the directory properties are separated by the ';' character.

```json
{
	"jobName": "DEFAULT_CRON_JOB",
	"jobStatus": "COMPLETED",
	"details": "DIRECTORY=C:/temp;CRON=0/10 * * * * ?;BUCKET=teste¢DIRECTORY=C:/temp2;CRON=0/10 * * * * ?;BUCKET=teste",
	"createdTime": "2022-10-12T22:26:05+0000",
	"endTime": "2022-10-12T22:26:08+0000",
	"files": [{
		"fileName": "xyz.zip",
		"url": "https://..."
	}],
	"exceptions": []
}
```

**Sample JSON error:**

```json
{
  "jobName": "DEFAULT_CRON_JOB",
  "jobStatus": "FAILED",
  "details": "DIRECTORY=C:/temp;CRON=0/10 * * * * ?;BUCKET=teste",
  "createdTime": "2022-10-12T22:57:54+0000",
  "endTime": "2022-10-12T22:57:57+0000",
  "files": [{
  	"fileName": "xyz.abc",
  	"url": "https://...."
  },
  {
  	"fileName": "xyz.abc",
  	"url": "https://...."
  }]
  "exceptions": [
    {
      "cause": null,
      "stackTrace": [
        {
          "classLoaderName": null,
          "moduleName": null,
          "moduleVersion": null,
          "methodName": "lambda$parseJobExecution$0",
          "fileName": "Hook.java",
          "lineNumber": 68,
          "className": "dev.alexmaycon.bucketservice.hook.model.Hook",
          "nativeMethod": false
        }
      ],
      "message": "It's a test!!!!",
      "suppressed": [],
      "localizedMessage": "It's a test!!!!"
    }
  ]
}
```

### Sending email with Sendgrid

If you have a Twilio Sendgrid account for sending emails, you can configure it using your account's API key to enable receiving email notifications.

If the creation of a pre-authenticated request is enabled, the file link will be sent in the email.

To enable email sending, simply add to your `application.properties` file:

```
service.email.sendgrid.apiKey=paste-your-api-key-here
service.email.sender=from@email.com
service.email.recipients[0]=to@email.com
```

You can configure any recipients you want:

```
service.email.sendgrid.apiKey=
service.email.sender=from@email.com
service.email.recipients[0]=to1@email.com
service.email.recipients[1]=to2@email.com
service.email.recipients[2]=to3@email.com
```



## Settings

Installation folder structure:

```
root
│   upload-bucket-service.jar
│   .oci
|   application.properties
```

### application.properties

**Attention:** You must have at least one directory using the global `service.cron`.

| Property                                 | Description                                                                                             | Required | Default Value             | Type    |
|------------------------------------------|---------------------------------------------------------------------------------------------------------|----------|---------------------------|---------|
| service.nameDefaultJob                   | Default job name                                                                                        | No       | "DEFAULT_CRON_JOB"        | String  |
| service.cron                             | Default cron expression to all jobs execution                                                           | No       | "0/10 * * * * ?"          | String  |
| service.email.sendgrid.apiKey            | Sendgrid API Key                        																 | No       |                           | String  |
| service.email.sender                     | Sender e-mail                          																 | No       |                           | String  |
| service.email.recipients[*]              | Recipients e-mail                          															 | No       |                           | String  |
| service.hook                             | API endpoint URL at which to be notified (POST) at the end of the JOB execution.                        | No       |                           | String  |
| service.hookContentType                  | Media type (json/xml)                                                                                   | No       | "application/json"        | String  |
| service.attemptsFailure                  | Number of attempts when a failure occurs                                                                | No       | 1                         | int     |
| service.oci.profile                      | Profile session of .oci configuration                                                                   | No       | "DEFAULT"                 | String  |
| service.oci.bucket                       | OCI Bucket name                                                                                         | **Yes**  |                           | String  |
| service.oci.generatePreauthenticatedUrl  | Enable/disable creation of pre-authenticated URLs for the object in OCI                                 | No       | false                          | Boolean  |
| service.oci.compartmentOcid              | Compartment OCID - if you wanted to create the bucket in a specific compartment.                        | No       |                           
| String  |
| service.folders[*]                       | Folders configuration                                                                                   | **Yes**  |                           | List    |
| service.folders[*].jobName               | Job name for current folder                                                                             | No       |                           | String  |
| service.folders[*].directory             | Folder path (need to include escape character for \ on Windows)                                         | **Yes**  |                           | String  |
| service.folders[*].cron                  | Cron expression specifies to the folder                                                                 | No       | Value from *service.cron* | String  |
| service.folders[*].overwriteExistingFile | Enable/disable file overwriting                                                                         | No       | false                     | boolean |
| service.folders[*].enabled               | Enables/disables folder processing.                                                                     | No       | true                      | boolean |
| service.folders[*].mapToBucketDir        | Set the directory to use in bucket. Leave empty to use root.                                            | No       |                           | String  |
| service.folders[*].oci.profile           | Profile session of .oci configuration (apply only to folder)                                            | No       | "DEFAULT"                 | String  |
| service.folders[*].oci.bucket            | OCI Bucket name (apply only to folder)                                                                  | No       |                           | String  |
| service.folders[*].oci.compartmentOcid   | Compartment OCID - if you wanted to create the bucket in a specific compartment. (apply only to folder) | No       |                           | String  |
| service.folders[*].generatePreauthenticatedUrl  | Generate Pre-authenticated URL to access the object (sended in hook and e-mail).		         | No       | false                           | boolean |
| service.email.sendgrid.apiKey            | API key for your Twilio Sendgrid account                                                                | No         |                           | String  |
| service.email.sendgrid.sender            | Sender's email                                                                                          | No         |                           | String  |
| service.email.sendgrid.recipients[*]     | Recipient email(s)                                                                                      | No         |                           | String  |
| service.zip.enabled                      | Enable/disable file compression for ZIP                                                                 | No         | false                          | Boolean  |
| service.zip.password                     | Password for encrypting the ZIP file. You can also set it using the `UBS_ZIP_PWD` environment variable | No         |                           | String  |

##  About OCI API key (.oci)

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

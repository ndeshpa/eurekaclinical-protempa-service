# Eureka! Clinical Protempa Service
[Georgia Clinical and Translational Science Alliance (Georgia CTSA)](http://www.georgiactsa.org), [Emory University](http://www.emory.edu), Atlanta, GA

## What does it do?
It provides backend services for managing phenotypes, cohorts and running phenotyping jobs.

Latest release: [![Latest release](https://maven-badges.herokuapp.com/maven-central/org.eurekaclinical/eurekaclinical-protempa-service/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.eurekaclinical/eurekaclinical-protempa-service)

## Version 1.0
This service is a refactoring of the eureka-protempa-etl module of the eureka project. It replaces the eureka-protempa-etl module. The current functionality is the same as in the last release of the eureka project.

## Build requirements
* [Oracle Java JDK 8](http://www.oracle.com/technetwork/java/javase/overview/index.html)
* [Maven 3.2.5 or greater](https://maven.apache.org)

## Runtime requirements
* [Oracle Java JRE 8](http://www.oracle.com/technetwork/java/javase/overview/index.html)
* [Tomcat 7](https://tomcat.apache.org)
* One of the following relational databases:
  * [Oracle](https://www.oracle.com/database/index.html) 11g or greater
  * [PostgreSQL](https://www.postgresql.org) 9.1 or greater
  * [H2](http://h2database.com) 1.4.193 or greater (for testing)
  
## REST APIs

### `/api/protected/users`
Manages registering a user with this service for authorization purposes.

#### Role-based authorization
Call-dependent

#### Requires successful authentication
Yes

#### User object
Properties:
* `id`: unique number identifying the user (set by the server on object creation, and required thereafter).
* `username`: required username string.
* `roles`: array of numerical ids of roles.

#### Calls
All calls use standard names, return values and status codes as specified in the [Eureka! Clinical microservice specification](https://github.com/eurekaclinical/dev-wiki/wiki/Eureka%21-Clinical-microservice-specification)

##### GET `/api/protected/users`
Returns an array of all User objects. Requires the `admin` role.

##### GET `/api/protected/users/{id}`
Returns a specified User object by the value of its id property, which is unique. Requires the `admin` role to return any user record. Otherwise, it will only return the user's own record.

##### GET `/api/protected/users/byname/{username}`
Returns a specified User object by its username, which is unique. Requires the `admin` role to return any user record. Otherwise, it will only return the user's own record.

##### GET `/api/protected/users/me`
Returns the User object for the currently authenticated user.

##### POST `/api/protected/users/`
Creates a new user. The User object is passed in as the body of the request. Returns the URI of the created User object. Requires the `admin` role.

##### PUT `/api/protected/users/{id}`
Updates the user object with the specified id. The User object is passed in as the body of the request. Requires the `admin` role.

### `/api/protected/roles`
Manages roles for this service. It is read-only.

#### Role-based authorization
No

#### Requires successful authentication
Yes

#### Role object
Properties:
* `id`: unique number identifying the role.
* `name`: the role's name string.

#### Calls
All calls use standard names, return values and status codes as specified in the [Eureka! Clinical microservice specification](https://github.com/eurekaclinical/dev-wiki/wiki/Eureka%21-Clinical-microservice-specification)

##### GET `/api/protected/roles`
Returns an array of all Role objects.

##### GET `/api/protected/roles/{id}`
Returns a specified Role object by the value of its id property, which is unique.

##### GET `/api/protected/roles/byname/{name}`
Returns a specified Role object by its name, which is unique.
  
### `/api/protected/jobs`
Manages phenotyping jobs.

#### Role-based authorization
Must have `research` role.

#### Requires successful authentication
Yes

#### JobSpec object
Used to submit a job request.

Properties:
* `sourceConfigId`: required string containing the name of the data source.
* `destinationId`: required string containing the name of the action.
* `dateRangePhenotypeKey`: optional unique key of a phenotype or concept on which to constrain the date range.
* `earliestDate`: optional timestamp, as milliseconds since the epoch, indicating the lower bound of the date range.
* `earliestDateSide`: optional string indicating on which side of the date range phenotype's interval to apply the earliest date; required if a value for `earliestDate` is specified; may be:
  * `START`: the beginning of the interval.
  * `FINISH`: the end of the interval.
* `latestDate`: optional timestamp, as milliseconds since the epoch, indicating the upper bound of the date range.
* `latestDateSide`: string indicating on which side of the date range phenotype's interval to apply the latest date; required if a value for `latestDate` is specified; may be:
  * `START`: the beginning of the interval.
  * `FINISH`: the end of the interval.
* `updateDate`: boolean indicating whether to update or replace data:
  * `true`: update data
  * `false`: replace data
* `prompts`: an array of SourceConfig objects containing any parameters for accessing the specified data source (see below).
* `propositionIds`: the keys of the data and/or phenotypes to retrieve from the data source.
* `name`: optional name for the job.

#### SourceConfig object
For specifying values of a source config's parameters.

Properties:
* `id`: the unique id string of the source config.
* `dataSourceBackends`: an array representing the data source backends that are being parameterized:
  * `id`: the id string of the data source backend.
  * `options`: an array of the parameters to set:
    * `name`: the unique name of the parameter.
    * `value`: the value of the parameter.

#### Job object
Created internally when a job is created. This object is read-only.

Properties:
* `id`: unique number identifying the job.
* `startTimestamp`: required timestamp, as milliseconds since the epoch, representing when the job began execution.
* `finishTimestamp`: required timestamp, as milliseconds since the epoch, representing when job execution ended.
* `sourceConfigId`: required string containing the name of the data source.
* `destinationId`: required string containing the name of the action.
* `username`: required string containing the username of who created the job.
* `status`: required string indicating the current status of the job:
  * `STARTING`: job is received but has not started yet.
  * `VALIDATING`: job is being validated.
  * `VALIDATED`: job has passed validation.
  * `STARTED`: job has started execution.
  * `COMPLETED`: job has completed without error.
  * `WARNING`: job has thrown a non-fatal warning.
  * `ERROR`: job has thrown a fatal error.
  * `FAILED`: job has completed in error.
* `jobEvents`: an array of JobEvent objects indicating job status (see below).
* `links`: an array of Link objects that point to resources created by the job (see below).
* `getStatisticsSupported`: boolean indicating whether the `/api/protected/jobs/{id}/stats` call is supported for this job.

#### JobEvent object
Represents events occurring during the execution of a job. This object is read-only.

Properties:
* `id`: unique number identifying the job event.
* `status`: the status of the job.
  * `STARTING`: job is received but has not started yet.
  * `VALIDATING`: job is being validated.
  * `VALIDATED`: job has passed validation.
  * `STARTED`: job has started execution.
  * `COMPLETED`: job has completed without error.
  * `WARNING`: job has thrown a non-fatal warning.
  * `ERROR`: job has thrown a fatal error.
  * `FAILED`: job has completed in error.
* `message`: optionally provides additional descriptive information for the job event.
* `exceptionStackTrace`: populated in job events with `ERROR` status with a stack trace.
* `timeStamp`: the timestamp, as milliseconds since the epoch, when this event occurred.

#### Link object
Represents a hyperlink to a resource created by the job.

Properties:
* `url`: required URL of the hyperlink.
* `displayName`: optional name for the link to display in a user interface.

#### Statistics object
Represents summary statistics of the resource created by the job.

Properties:
* `numberOfKeys`: the number of patients in the resource.
* `counts`: a map of concept or phenotype to the number of times it appears in the resource.
* `childrenToParents`: a map from each concept or phenotype to its parent concepts or phenotypes.

#### Calls
Uses status codes as specified in the [Eureka! Clinical microservice specification](https://github.com/eurekaclinical/dev-wiki/wiki/Eureka%21-Clinical-microservice-specification).

##### POST `/api/protected/jobs`
Submits a job. A JobSpec object is passed in as the body of the request. Returns the URI representing the corresponding Job object.

##### GET `/api/protected/jobs/{id}`
Gets the Job with the specified numerical unique id.
###### Example:
URL: https://localhost:8443/eurekaclinical-protempa-service/api/protected/jobs/1

Return: 
```
{ "id":1,
  "startTimestamp":1483992452240,
  "sourceConfigId":"Spreadsheet",
  "destinationId":"Load into i2b2 on localhost with Eureka ontology",
  "username":"superuser",
  "status":"COMPLETED",
  "jobEvents":[
    { "id":1,
      "status":"STARTED",
      "exceptionStackTrace":null,
      "timeStamp":1483992452303,
      "message":"Processing started"},       
    { "id":2,
      "status":"COMPLETED",
      "exceptionStackTrace":null,
      "timeStamp":1483992511412,
      "message":"Processing completed without error"}],
  "links":[],
  "getStatisticsSupported":false,
  "finishTimestamp":148399251141
}
```

##### GET `/api/protected/jobs[?order=asc|desc]`
Gets all jobs for the current user. Optionally, you can specify whether jobs will be returned in ascending or descending order.

###### Example:
URL: https://localhost:8443/eurekaclinical-protempa-service/api/protected/jobs?order=desc

Return:
```
[
  { "id":2,
    "startTimestamp":1483992674788,
    "sourceConfigId":"Spreadsheet",
    "destinationId":"Load into i2b2 on localhost with Eureka ontology",
    "username":"superuser",
    "status":"COMPLETED",
    "jobEvents": [
      { "id":3,
        "status":"STARTED",
        "exceptionStackTrace":null,
        "timeStamp":1483992674792,
        "message":"Processing started"},
      { "id":4,
        "status":"COMPLETED",
        "exceptionStackTrace":null,
        "timeStamp":1483992752190,
        "message":"Processing completed without error"}
    ],
    "links":[],
    "getStatisticsSupported":false,
    "finishTimestamp":1483992752190
  },
  { "id":1,
    "startTimestamp":1483992452240,
    "sourceConfigId":"Spreadsheet",
    "destinationId":"Load into i2b2 on localhost with Eureka ontology",
    "username":"superuser",
    "status":"COMPLETED",
    "jobEvents": [
      { "id":1,
        "status":"STARTED",
        "exceptionStackTrace":null,
        "timeStamp":1483992452303,
        "message":"Processing started"},
      { "id":2,
        "status":"COMPLETED",
        "exceptionStackTrace":null,
        "timeStamp":1483992511412,
        "message":"Processing completed without error"}
    ],
    "links":[],
    "getStatisticsSupported":false,
    "finishTimestamp":1483992511412
  }
]
```

##### GET `/api/protected/jobs/status?jobId=jobId&userId=userId&state=foo&from=bar&to=baz`
Gets an array of all Jobs for the current user, optionally filtered by job id, user id, state (status) and/or date range (from date, to date).

##### GET `/api/protected/jobs/{jobId}/stats[/{key}]`
Gets a Statistics object for the specified Job, optionally constraining the results to statistics about the concept or phenotype with the specified key.

##### GET `/api/protected/jobs/latest`
Gets the most recently submitted Job for the user.

###### Example:
URL: https://localhost:8443/eurekaclinical-protempa-service/api/protected/jobs/latest

Returns:
```
[
  { "id":2,
    "startTimestamp":1483992674788,
    "sourceConfigId":"Spreadsheet",
    "destinationId":"Load into i2b2 on localhost with Eureka ontology",
    "username":"superuser",
    "status":"COMPLETED",
    "jobEvents":[
      { "id":3,
        "status":"STARTED",
        "exceptionStackTrace":null,
        "timeStamp":1483992674792,
        "message":"Processing started" },
        { "id":4,
          "status":"COMPLETED",
          "exceptionStackTrace":null,
          "timeStamp":1483992752190,
          "message":"Processing completed without error" }
    ],
    "links":[],
    "getStatisticsSupported":false,
    "finishTimestamp":1483992752190}
]
```

### `/api/protected/file`
Manages data file uploads.

#### Role-based authorization
Must have `research` role.

#### Requires successful authentication
Yes

#### Calls
Uses status codes as specified in the [Eureka! Clinical microservice specification](https://github.com/eurekaclinical/dev-wiki/wiki/Eureka%21-Clinical-microservice-specification).

##### POST /protected/file/upload/{sourceConfigId}/{sourceId}
Submit a multipart form containing a file with form parameter name `file` for the source config with the specified unique name (`sourceConfigId`). The sourceId is a source config-specific identifier for the file.

### `/api/protected/destinations`
Manages job actions.

#### Role-based authorization
Must have `research` role.

#### Requires successful authentication
Yes

#### Destination object
Destinations are job actions that create a resource. They all have the following properties:

Properties:
* `id`: unique number identifying the cohort (set by the server on object creation, and required thereafter).
* `type`: always must have value `COHORT`.
* `name`: required unique name of the cohort.
* `description`: an optional description of the cohort.
* `links`: an array of Link objects that point to resources related to the cohort see Link object above.
* `ownerUserId`: required username string of the owning user.
* `read`: required boolean indicating whether the user may read this object.
* `write`: required boolean indicating whether the user may update this object.
* `execute`: required boolean indicating whether the user may use this cohort specification as an action.
* `createdAt`: timestamp, in milliseconds since the epoch, indicating when this cohort specification was created; populated server-side.
* `updatedAt`: timestamp, in milliseconds since the epoch, indicating when this cohort specification was updated; populated server-side.
* `getStatisticsSupported`: required boolean indicating whether the resource created by a job executing this action supports getting statistics.
* `jobConceptListSupported`: required boolean indicating whether a job executing this action has a concept/phenotype list.
* `requiredConcepts`: any concepts or phenotypes that must be in the concept list.

#### CohortDestination object
Creates a patient set containing only patients who match the specified criteria.

Properties:
* `cohort`: a required Cohort object (see below).

#### Cohort object
A specification of a patient cohort in terms of concepts and phenotypes.

Properties:
* `id`: unique numerical id of the cohort (set by the server on object creation, and required thereafter).
* `node`: required Literal or BinaryOperator object (see below). Use a Literal object if the cohort is defined by a single concept or phenotype. If the cohort is defined by multiple concepts or phenotypes, use a chain of BinaryOperator objects ending with a Literal object.

#### BinaryOperator object
`ANDs` two nodes together.

Properties:
`leftNode`: required Literal object.
`op`: the operator, always `AND`.
`rightNode`: required BinaryOperator or Literal object.

#### Literal object
Represents a concept or phenotype included in a cohort definition.

Properties:
* `name`: required unique key of the concept or phenotype.
* `start`: always `null`.
* `finish`: always `null`.

#### I2b2Destination object
Populates an i2b2 data warehouse.

Properties:
No additional properties

#### Neo4jDestination object
Populates a Neo4j database.

Properties:
No additional properties.

#### Calls
Uses status codes as specified in the [Eureka! Clinical microservice specification](https://github.com/eurekaclinical/dev-wiki/wiki/Eureka%21-Clinical-microservice-specification).

##### GET `/api/protected/destinations[?type=[I2B2,COHORT,PATIENT_SET_SENDER]`
Gets all data destinations visible to the current user.  Optionally, filter the returned destinations by type:
* `I2B2`: i2b2 database destination.
* `COHORT`: Cohort specified in the cohorts screens.
* `PATIENT_SET_SENDER`: patient set sender.

###### Example:
URL: https://localhost:8443/eurekaclinical-protempa-service/api/protected/destinations?type=I2B2

Returns:
```
[
  { "type":"I2B2",
    "id":1,
    "name":"Load into i2b2 on localhost with Eureka ontology",
    "description":null,
    "phenotypeFields":null,
    "links[],
    "ownerUserId":1,
    "read":true,
    "write":true,
    "execute":true,
    "getStatisticsSupported":true,
    "jobConceptListSupported":true,
    "requiredConcepts":["Encounter"],
    "created_at":1430774820000,
    "updated_at":1430774820000 }
]
```

##### GET `/api/protected/destinations/{id}`
Gets the data destination with the specified unique name (id), if it is visible to the current user.

##### POST `/api/protected/destinations`
Create a new data destination, returning a URI representing the created destination object.

##### PUT `/api/protected/destinations`
Updates an existing data destination
Returns nothing.

##### DELETE `/api/protected/destinations/{id}`
Deletes the destination with the specified unique numerical id. Returns nothing.

### `/api/protected/concepts`
System concepts provided by the system.

#### Role-based authorization
Must have `researcher` role.

#### Requires successful authentication
Yes

#### Concept object
Representation of concepts provided by the system. They are read-only.

Properties:
* `id`: always `null`.
* `key`: required unique name of the concept.
* `userId`: always `null`.
* `description`: optional description of the concept.
* `displayName`: optional user-visible name for the concept.
* `inSystem`: required boolean indicating whether it is a concept provided by the system. Always `true`.
* `created`: required timestamp, in milliseconds since the epoch, indicating when the concept was created (set by the server).
* `lastModified`: timestamp, in milliseconds since the epoch, indicating when the concept was last modified (set by the server).
* `summarized`: read-only boolean indicating whether the concept was retrieved with the `summarize` query parameter set to `true`.
* `type`: the type of concept, always `SYSTEM`.
* `internalNode`: read-only boolean indicating whether the concept has any children.
* `systemType`: required, one of `CONSTANT`, `EVENT`, `PRIMITIVE_PARAMETER`, `LOW_LEVEL_ABSTRACTION`, `COMPOUND_LOW_LEVEL_ABSTRACTION`, `HIGH_LEVEL_ABSTRACTION`, `SLICE_ABSTRACTION`, `SEQUENTIAL_TEMPORAL_PATTERN_ABSTRACTION`, `CONTEXT`.
* `children`: an array of Concept objects.
* `isParent`: whether this concept has any children.
* `properties`: array of property names.

#### Calls
Uses status codes as specified in the [Eureka! Clinical microservice specification](https://github.com/eurekaclinical/dev-wiki/wiki/Eureka%21-Clinical-microservice-specification).

##### GET `/api/protected/concepts[?summarize=true]`
Returns the top-level system concepts accessible by the current user. Optionally, return each concept in a summarized form suitable for listing.

###### Example:
URL: https://localhost:8443/eurekaclinical-protempa-service/api/protected/concepts

Returns: 
```
[
  { "type":"SYSTEM",
    "id":null,
    "key":"Patient",
    "userId":null,
    "description":"",
    "displayName":"Patient",
    "inSystem":true,
    "created":null,
    "lastModified":null,
    "summarized":true,
    "internalNode":false,
    "systemType":"CONSTANT",
    "children":null,
    "properties":["patientId"],
    "parent":false},
  { "type":"SYSTEM",
    "id":null,
    "key":"PatientDetails",
    "userId":null,
    "description":"",
    "displayName":"Patient Details",
    "inSystem":true,
    "created":null,
    "lastModified":null,
    "summarized":true,
    "internalNode":false,
    "systemType":"CONSTANT",
    "children":null,
    "properties": [
      "ageInYears",
      "dateOfBirth",
      "dateOfDeath",
      "gender",
      "language",
      "maritalStatus",
      "race",
      "vitalStatus",
      "patientId"
    ],
    "parent":false},
  { "type":"SYSTEM",
    "id":null,
    "key":"Encounter",
    "userId":null,
    "description":"",
    "displayName":"Encounter",
    "inSystem":true,
    "created":null,
    "lastModified":null,
    "summarized":true,
    "internalNode":false,
    "systemType":"EVENT",
    "children":null,
    "properties": ["age","type","encounterId"],
    "parent":false},
  { "type":"SYSTEM",
    "id":null,
    "key":"VitalSign",
    "userId":null,
    "description":"",
    "displayName":"Vital Sign",
    "inSystem":true,
    "created":null,
    "lastModified":null,
    "summarized":true,
    "internalNode":true,
    "systemType":"PRIMITIVE_PARAMETER",
    "children":null,
    "properties":[],
    "parent":true}
]
```

##### POST `/api/protected/concepts[?summarize=true]`
Retrieves the concepts enumerated in the form body. Optionally, returns each concept in a summarized form suitable for listing.

Form parameters:
* key: The keys of the system concepts of interest (optional). If omitted, the empty list is returned.
* summarize: yes or no if you want returned concepts in a summarized form suitable for listing (optional).

##### GET `/api/protected/concepts/{key}`
Gets the requested system concept with the specified key or the 404 (NOT FOUND) status code if no such system concept exists and is accessible to the current user. 

###### Example:
URL: https://localhost:8443/eurekaclinical-protempa-service/api/protected/concepts/Patient

Returns:
```
{ "type":"SYSTEM",
  "id":null,
  "key":"Patient",
  "userId":null,
  "description":"",
  "displayName":"Patient",
  "inSystem":true,
  "created":null,
  "lastModified":null,
  "summarized":false,
  "internalNode":false,
  "systemType":"CONSTANT",
  "children":[],
  "properties":["patientId"],
  "parent":false }
```

##### GET `/api/protected/concepts/propsearch/{searchKey}`
Gets the concepts with the specified text in their display name, case insensitive.

###### Example:
URL: https://localhost:8443/eurekaclinical-protempa-service/api/protected/concepts/propsearch/ICD9%20Procedure%20Codes

Returns: 
```
[
  { "type":"SYSTEM",
    "id":null,
    "key":"ICD9:Procedures",
    "userId":null,
    "description":"",
    "displayName":"ICD9 Procedure Codes",
    "inSystem":true,
    "created":null,
    "lastModified":null,
    "summarized":true,
    "internalNode":true,
    "systemType":"EVENT",
    "children":null,
    "properties":[],
    "parent":true}
]
```

##### GET `/api/protected/concepts/search/{searchKey}`
Gets an array of the keys of the system concepts with the specified text in their display name, case insensitive.

###### Example:
URL: https://localhost:8443/eurekaclinical-protempa-service/api/protected/concepts/search/ICD10:Diagnoses

Returns:
```
["ICD10:Diagnoses", "ICD10:S00-T88", "ICD10:S00-S09", ...]

## Building it
See the parent project's [README.md](https://github.com/eurekaclinical/eureka/blob/master/README.md).

## Performing system tests
See the parent project's [README.md](https://github.com/eurekaclinical/eureka/blob/master/README.md).

## Installation
### Configuration
This webapp is configured using a properties file located at `/etc/eureka/application.properties`. It supports the following properties:
* `cas.url`: https://hostname.of.casserver:port/cas-server
* `eureka.etl.url`: URL of the server running the backend; default is https://localhost:8443/eurekaclinical-protempa-service.
* `eureka.etl.threadpool.size`: the number of threads in the ETL threadpool; default is 4.
* `eureka.etl.callbackserver`: URL of the server running the backend; default is https://localhost:8443.

A Tomcat restart is required to detect any changes to the configuration file.

### WAR installation
1) Stop Tomcat.
2) Remove any old copies of the unpacked war from Tomcat's webapps directory.
3) Copy the warfile into the Tomcat webapps directory, renaming it to remove the version if necessary. For example, rename `eurekaclinical-protempa-service-1.0.war` to `eurekaclinical-protempa-service.war`.
4) Start Tomcat.

## Maven dependency
```
<dependency>
    <groupId>org.eurekaclinical</groupId>
    <artifactId>eurekaclinical-protempa-service</artifactId>
    <version>version</version>
</dependency>
```

## Developer documentation
* [Javadoc for latest development release](http://javadoc.io/doc/org.eurekaclinical/eurekaclinical-protempa-service) [![Javadocs](http://javadoc.io/badge/org.eurekaclinical/eurekaclinical-protempa-service.svg)](http://javadoc.io/doc/org.eurekaclinical/eurekaclinical-protempa-service)

## Getting help
Feel free to contact us at help@eurekaclinical.org.

# Service Overview

SSAI Front end it's web service for sharing modified HLS playlists for clients.

It can get url to HLS stream and return m3u8 playlists with some modifications.

## Getting Started

These instructions will help you to understand the service and deploy it on your local machine for development and testing purposes.


### Tech stack
1. Front End
```
HTML
CSS
JavaScript
Vue.js
REST API
```
2. Backend
```
Gradle
Java
Spring Boot
Spring Framework
Spring REST
Spring Data JPA
GCP SQL
GCP Pub Sub 
```  

### Clone & setup

What things you need to install the software and how to install them

1. Clone project locally:  
```
git@gitlab.postindustria.com:ssai/platform.git
```

2. Build executable file

```
cd front
./gradlew clean bootJar
```
3. Command line attributes

```
usage: java -jar front-0.0.1-SNAPSHOT.jar
```

4. Run example

```
java -jar build/libs/front-0.0.1-SNAPSHOT.jar
```
###Web service api integration

```
//TODO: API description
```
### Environment
For local usage you must configure PostgreSQL and Google Cloud Platform emulator.
All instructions are relevant for MAC OSX.

PostgreSQL:
```
brew install postgresql
brew services start postgresql
sudo psql -U <USER_NAME> -d postgres
CREATE DATABASE <DB_NAME>;
CREATE USER <USER_NAME> WITH PASSWORD '<PASSWORD_NAME>';
exit;
```

GCP Pub Sub emulator:
```
install GCP emulators from https://cloud.google.com/sdk/docs/quickstart-macos 
gcloud init
    - login to google account
    - select GCP project
gcloud components install pubsub-emulator
gcloud components update
gcloud beta emulators pubsub start
```



### Configuration
All config values placed in section App of /resources/application.properties file

```
## default connection pool
spring.datasource.hikari.connectionTimeout - database connection timeout
spring.datasource.hikari.maximumPoolSize - database connections pool size

## PostgreSQL
spring.datasource.url - JDBC url for database
spring.datasource.username - DB account's username
spring.datasource.password - DB account's password

spring.jpa.show-sql - true for debugging SQL, must be removed in production 
spring.jpa.hibernate.ddl-auto=create - recreate DB each time after restart, must be removed in production

## Pub/Sub
spring.cloud.gcp.pubsub.emulatorHost - url for Pub Sub emulator, must be removed in production
spring.cloud.gcp.project-id - Google Cloud Project's identifier
spring.cloud.gcp.credentials.location - path to JSON file with credentials for GCP
 
```
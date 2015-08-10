Platform
========

The core platform.

# Building

Ensure you have a base environment established using the bootstrap from the *support* repository.

All projects can then be built using Maven:

```
mvn clean install
```

# Components

 - **control-centre** - administration web application.
 - **lightstreamer-adapter** - glue adapter for Lightstreamer messaging.
 - **platform-acceptance-tests** - Fitnesse test suite for older Platform components.
 - **platform-api** - public API to platform functionality.
 - **platform-db** - DB schema for Platform DB persistence.
 - **platform-grid** - the core data grid and game server.
 - **platform-shared** - reusable code and configuration within Platform projects.
 - **platform-worker** - statements workers for Platform tasks.
 - **standalone-server** - test game server for game development.

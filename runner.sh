#!/bin/bash
java -jar target/floodlight.jar &
java -jar target/floodlight.jar -cf src/main/resources/floodlightNodeBackup.properties &
java -jar target/floodlight.jar -cf src/main/resources/floodlightNodeBackup3.properties &
java -jar target/floodlight.jar -cf src/main/resources/floodlightNodeBackup4.properties

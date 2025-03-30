#!/bin/bash

# Compile all Java files in the simulations folder and copy dependencies
mvn -B compile dependency:copy-dependencies

# Run each simulation
java -cp "target/classes:target/dependency/*" simulations.MigrationBestFitPolicy
java -cp "target/classes:target/dependency/*" simulations.MigrationRandomPolicy
java -cp "target/classes:target/dependency/*" simulations.MigrationWrostFitPolicy

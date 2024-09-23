#!/bin/bash

# Uncomment to build all
# ./mvnw -T 0.5C -DskipDocs -DskipTests -DskipITs -DskipExtensionValidation -Dskip.gradle.tests -Dgradle.cache.local.enabled=true clean install

./mvnw test -Ddevelocity.cache.local.enabled=false -Dforbiddenapis.skip=true -Denforcer.skip=true -pl extensions/hibernate-orm/deployment -Dtest=PublicFieldAccessInheritanceTest -Dsurefire.rerunFailingTestsCount=10000

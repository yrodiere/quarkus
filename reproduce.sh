#!/bin/bash

# Uncomment to build all
# ./mvnw -T 0.5C -DskipDocs -DskipTests -DskipITs -DskipExtensionValidation -Dskip.gradle.tests -Dgradle.cache.local.enabled=true clean install

i=1
while ./mvnw test -Ddevelocity.cache.local.enabled=false -Dforbiddenapis.skip=true -Denforcer.skip=true -pl extensions/hibernate-orm/deployment -Dtest=PublicFieldAccessInheritanceTest
do
	echo "Success :("
	# Avoid filling up the disk
	rm -rf extensions/hibernate-orm/deployment/target/debug/
	(( i++ ))
	echo "Trying again -- execution #$i"
done

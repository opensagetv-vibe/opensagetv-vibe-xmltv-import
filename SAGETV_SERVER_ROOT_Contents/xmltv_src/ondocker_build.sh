#!/bin/bash
javac -Xlint:deprecation -Xlint:-options -source 8 -target 8 -classpath ../Sage.jar -d ./ Channel.java Init.java Show.java ShowIdGenerator.java XMLInputStreamFilter.java XMLTVImportPlugin.java && jar cvf ../JARs/XMLTVImportPlugin.jar xmltv

#!/bin/sh

# clear jar links
find -name '*.jar' | xargs rm -f

# create mosaic links
MOSAIC_VERSION="1.0.0-SNAPSHOT"

# mosaic modules
ln -s ../../org.mosaic.convert/target/org.mosaic.convert-${MOSAIC_VERSION}.jar org.mosaic.convert.jar

# create demo
ln -s ../../org.mosaic.demo/org.mosaic.demo.demo1/target/org.mosaic.demo.demo1-${MOSAIC_VERSION}.jar org.mosaic.demo.demo1.jar
ln -s ../../org.mosaic.demo/org.mosaic.demo.demo2/target/org.mosaic.demo.demo2-${MOSAIC_VERSION}.jar org.mosaic.demo.demo2.jar

# create 3rd party links
#ln -s ~/.m2/repository/com/google/guava/guava/16.0.1/guava-16.0.1.jar com.google.guava.jar
#ln -s ~/.m2/repository/org/hsqldb/hsqldb/2.3.1/hsqldb-2.3.1.jar org.hsqldb.jar
#ln -s ~/.m2/repository/com/fasterxml/classmate/0.8.0/classmate-0.8.0.jar com.fasterxml.classmate.jar
#ln -s ~/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.3.0/jackson-core-2.3.0.jar com.fasterxml.jackson.core.jar
#ln -s ~/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.3.0/jackson-annotations-2.3.0.jar com.fasterxml.jackson.core.annotations.jar
#ln -s ~/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.3.0/jackson-databind-2.3.0.jar com.fasterxml.jackson.core.databind.jar
#ln -s ~/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-csv/2.3.0/jackson-dataformat-csv-2.3.0.jar com.fasterxml.jackson.dataformat.csv.jar
#ln -s ~/.m2/repository/javax/el/javax.el-api/3.0.0/javax.el-api-3.0.0.jar javax.el.jar
#ln -s ~/.m2/repository/org/glassfish/web/javax.el/2.2.6/javax.el-2.2.6.jar org.glassfish.web.javax.el.jar

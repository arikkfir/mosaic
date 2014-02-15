#!/bin/sh

# clear jar links
find -name '*.jar' | xargs rm -f
mkdir -p mosaic thirdparty

# create mosaic links
MOSAIC_VERSION="1.0.0-SNAPSHOT"
pushd mosaic
for s in 'base' 'collections' 'conversion' 'expression' 'method' 'osgi' 'reflection' 'resource' 'xml'
do
    ln -s ../../../org.mosaic.utils/org.mosaic.utils.${s}/target/org.mosaic.utils.${s}-${MOSAIC_VERSION}.jar org.mosaic.utils.${s}.jar
done
for s in 'config' 'console' 'console.remote' 'dao' 'datasource' 'event' 'modules' 'pathwatchers' 'security' 'server' 'tasks' 'validation' 'web'
do
    ln -s ../../../org.mosaic.${s}/target/org.mosaic.${s}-${MOSAIC_VERSION}.jar org.mosaic.${s}.jar
done
popd

# create 3rd party links
pushd thirdparty
ln -s ~/.m2/repository/com/fasterxml/classmate/0.8.0/classmate-0.8.0.jar com.fasterxml.classmate.jar
ln -s ~/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.3.0/jackson-core-2.3.0.jar com.fasterxml.jackson.core.jar
ln -s ~/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.3.0/jackson-annotations-2.3.0.jar com.fasterxml.jackson.core.annotations.jar
ln -s ~/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.3.0/jackson-databind-2.3.0.jar com.fasterxml.jackson.core.databind.jar
ln -s ~/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-csv/2.3.0/jackson-dataformat-csv-2.3.0.jar com.fasterxml.jackson.dataformat.csv.jar
ln -s ~/.m2/repository/com/google/guava/guava/16.0.1/guava-16.0.1.jar com.google.guava.jar
ln -s ~/.m2/repository/javax/el/javax.el-api/3.0.0/javax.el-api-3.0.0.jar javax.el.jar
ln -s ~/.m2/repository/joda-time/joda-time/2.3/joda-time-2.3.jar org.joda-time.jar
ln -s ~/.m2/repository/org/apache/commons/commons-lang3/3.2.1/commons-lang3-3.2.1.jar org.apache.commons.lang3.jar
ln -s ~/.m2/repository/org/apache/felix/org.apache.felix.configadmin/1.8.0/org.apache.felix.configadmin-1.8.0.jar org.apache.felix.configadmin.jar
ln -s ~/.m2/repository/org/apache/felix/org.apache.felix.eventadmin/1.3.2/org.apache.felix.eventadmin-1.3.2.jar org.apache.felix.eventadmin.jar
ln -s ~/.m2/repository/org/apache/felix/org.apache.felix.log/1.0.1/org.apache.felix.log-1.0.1.jar org.apache.felix.log.jar
ln -s ~/.m2/repository/org/glassfish/web/javax.el/2.2.6/javax.el-2.2.6.jar org.glassfish.web.javax.el.jar
ln -s ~/.m2/repository/org/slf4j/jcl-over-slf4j/1.7.6/jcl-over-slf4j-1.7.6.jar jcl-over-slf4j.jar
ln -s ~/.m2/repository/org/slf4j/log4j-over-slf4j/1.7.6/log4j-over-slf4j-1.7.6.jar log4j-over-slf4j.jar
popd

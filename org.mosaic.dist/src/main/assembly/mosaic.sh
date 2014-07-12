#!/bin/sh
#
# Init script for Mosaic
#
# chkconfig: 345 99 20
# description: Mosaic Server
#
# processname: mosaic
# pidfile: /var/run/mosaic.pid
#
###[ Command line arguments: ]######################################################
#
#   start   Starts the server (in the background, returns BEFORE fully loaded)
#   stop    Stops the server (waits for it to shut down)
#   restart Restarts the server (waits for it to shutdown)
#   status  Shows the status of the server
#   debug   Starts the server in debug mode
#   run     Starts the server in the foreground
#
###[ Environment variables: ]#######################################################
#
#   MOSAIC_HOME         May point at your Mosaic home directory.
#   MOSAIC_JAVA_OPTS    Custom JVM options to pass to the server
#   MOSAIC_PID_FILE     May point to the run PID file used for tracking the Mosaic process
#
####################################################################################


####################################################################################
# Detect Mosaic home
####################################################################################
SOURCE="${BASH_SOURCE[0]}"
DIR="$( dirname "${SOURCE}" )"
while [ -h "${SOURCE}" ]
do
  SOURCE="$(readlink "${SOURCE}")"
  [[ ${SOURCE} != /* ]] && SOURCE="${DIR}/${SOURCE}"
  DIR="$( cd -P "$( dirname "${SOURCE}"  )" && pwd )"
done
MOSAIC_HOME="$( cd -P "$( dirname "${SOURCE}" )" && cd .. && pwd )"

####################################################################################
# Make sure required variables are set
####################################################################################
[ -z "${MOSAIC_JAVA_OPTS}" ] && MOSAIC_JAVA_OPTS=""
[ -z "${MOSAIC_SERVER_PROC_PTRN}" ] && MOSAIC_SERVER_PROC_PTRN="java.*${MOSAIC_HOME}.*-jar.*launcher\.jar"
[ -z "${MOSAIC_DEBUG_PORT}" ] && MOSAIC_DEBUG_PORT="5005"
[ -z "${MOSAIC_DEBUG_JAVA_OPTS}" ] && MOSAIC_DEBUG_JAVA_OPTS=""
[ -z "${MOSAIC_BACKGROUND}" ] && MOSAIC_BACKGROUND="yes"
[ -z "${MOSAIC_JMX_PORT}" ] && MOSAIC_JMX_PORT="7080"
[ -z "${MOSAIC_HOME_APPS}" ] && MOSAIC_HOME_APPS="${MOSAIC_HOME}/apps"
[ -z "${MOSAIC_HOME_BIN}" ] && MOSAIC_HOME_BIN="${MOSAIC_HOME}/bin"
[ -z "${MOSAIC_HOME_ETC}" ] && MOSAIC_HOME_ETC="${MOSAIC_HOME}/etc"
[ -z "${MOSAIC_HOME_LIB}" ] && MOSAIC_HOME_LIB="${MOSAIC_HOME}/lib"
[ -z "${MOSAIC_HOME_LOGS}" ] && MOSAIC_HOME_LOGS="${MOSAIC_HOME}/logs"
[ -z "${MOSAIC_HOME_SCHEMAS}" ] && MOSAIC_HOME_SCHEMAS="${MOSAIC_HOME}/schemas"
[ -z "${MOSAIC_HOME_WORK}" ] && MOSAIC_HOME_WORK="${MOSAIC_HOME}/work"
[ -z "${MOSAIC_PID_FILE}" ] && MOSAIC_PID_FILE="${MOSAIC_HOME_WORK}/mosaic.pid"


####################################################################################
# The 'start' function for running Mosaic
####################################################################################
start()
{
    #
    # If Mosaic is running already running, do nothing
    #
    pgrep -f "${MOSAIC_SERVER_PROC_PTRN}" > /dev/null
    if [ "0" = "$?" ] ; then
        echo "Mosaic server already running."
        exit 0
    fi

    #
    # If Mosaic is running already running, do nothing
    #
    pgrep -f "${MOSAIC_SERVER_PROC_PTRN}" > /dev/null
    if [ -f "${MOSAIC_PID_FILE}" ] ; then
        echo "Mosaic PID file exists even though server is not running. Deleting PID file."
        rm -f ${MOSAIC_PID_FILE}
    fi

    #
    # Create the final command line
    #
    CMDLINE="                                                           \
        java                                                            \
        -XX:-OmitStackTraceInFastThrow                                  \
        -XX:+PrintCommandLineFlags                                      \
        -Dcom.sun.management.jmxremote.port=${MOSAIC_JMX_PORT}          \
        -Dcom.sun.management.jmxremote.authenticate=false               \
        ${MOSAIC_DEBUG_JAVA_OPTS}                                       \
        ${MOSAIC_JAVA_OPTS}                                             \
        -Dorg.mosaic.home=${MOSAIC_HOME}                                \
        -Dorg.mosaic.home.apps=${MOSAIC_HOME_APPS}                      \
        -Dorg.mosaic.home.bin=${MOSAIC_HOME_BIN}                        \
        -Dorg.mosaic.home.etc=${MOSAIC_HOME_ETC}                        \
        -Dorg.mosaic.home.lib=${MOSAIC_HOME_LIB}                        \
        -Dorg.mosaic.home.logs=${MOSAIC_HOME_LOGS}                      \
        -Dorg.mosaic.home.schemas=${MOSAIC_HOME_SCHEMAS}                \
        -Dorg.mosaic.home.work=${MOSAIC_HOME_WORK}                      \
        -jar ${MOSAIC_HOME_BIN}/org.mosaic.core.jar"
    CMDLINE=`echo ${CMDLINE} | tr '\n' ' '`

    #
    # Log command line to script log
    #
    echo "`date` (`logname`): Starting Mosaic server with command line:" >> ${MOSAIC_HOME}/logs/script.log
    echo "    ${CMDLINE}" >> "${MOSAIC_HOME_LOGS}/script.log"

    #
    # Execute command line
    #
    if [ "yes" == "${MOSAIC_BACKGROUND}" ]; then
        echo -n $"Starting Mosaic server..."
        eval ${CMDLINE} >> ${MOSAIC_HOME_LOGS}/stdout.log 2>&1 "&"

        # Give it a chance to start...
        sleep 1

        # Find out the process ID (PID) and write it in the PID file
        PID=`pgrep -f "${MOSAIC_SERVER_PROC_PTRN}"`
        if [ "1" = "$?" ] ; then
            echo "FAILED"
            EXIT_CODE=1
        else
            echo ${PID} > ${MOSAIC_PID_FILE}
            echo "OK"
            EXIT_CODE=0
        fi

    else
        eval ${CMDLINE} >> ${MOSAIC_HOME_LOGS}/stdout.log 2>&1
        EXIT_CODE=$?
        echo "Mosaic server stopped with exit code ${EXIT_CODE}"
    fi
}


####################################################################################
# The 'status' function for determining if Mosaic is running
####################################################################################
status()
{
    PID=`pgrep -f "${MOSAIC_SERVER_PROC_PTRN}"`
    if [ "1" = "$?" ] ; then
        echo "Mosaic server is not running."
        EXIT_CODE=0
    else
        echo "Mosaic server is running (process ID is ${PID})"
        EXIT_CODE=1
    fi
 }


####################################################################################
# The 'stop' function for stopping Mosaic
####################################################################################
stop()
{
    if [[ -f ${MOSAIC_PID_FILE} ]] ; then
        echo -n $"Stopping Mosaic server..."
        echo "`date` (`logname`): Stopping Mosaic server" >> ${MOSAIC_HOME_LOGS}/script.log

        # Find the server PID
        export PID=`cat ${MOSAIC_PID_FILE}`

        # Start a timer, send the kill signal, and wait for the server to stop, aborting at the end of the timer
        kill ${PID}
        RC=0
        while [[ `ps -p ${PID}|wc -l` -ge 2 && RC -eq 0 ]] ; do
            sleep 1
            RC=$?
        done

        if [[ ${RC} -ne 0 ]] ; then
            echo "unable to stop Mosaic server. (pid=${PID})"
            echo "Attempting to kill by force."
            echo "`date` (`logname`): (soft kill failed) stopping Mosaic server by force" >> ${MOSAIC_HOME_LOGS}/script.log
            kill -9 ${PID}
        else
            rm -f ${MOSAIC_PID_FILE}
            echo "OK"
        fi
        EXIT_CODE=0
    else
        echo "Mosaic server not running (PID file does not exist at ${MOSAIC_PID_FILE})"
        EXIT_CODE=0
    fi
}


####################################################################################
# Main section: determine command and execute it accordingly
####################################################################################

# check PID can be created/removed
MOSAIC_PID_DIR=`dirname ${MOSAIC_PID_FILE}`
if [ ! -d ${MOSAIC_PID_DIR} ]; then
    mkdir ${MOSAIC_PID_DIR}
elif [ ! -w ${MOSAIC_PID_DIR} ]; then
    echo "Directory for PID file at ${MOSAIC_PID_FILE} is not writable by `whoami`."
    echo "Please add permissions for `whoami`, or change the MOSAIC_PID_FILE environment variable."
    exit 1
fi
if [ -f ${MOSAIC_PID_FILE} -a ! -w ${MOSAIC_PID_FILE} ]; then
    echo "PID file at ${MOSAIC_PID_FILE} is not writable by `whoami`."
    exit 1
fi

# Log the invocation
mkdir -p ${MOSAIC_HOME_LOGS}
echo "`date` (`logname`): Mosaic script has been run with commands: $*" >> ${MOSAIC_HOME_LOGS}/script.log

# Perform requested operation based on arguments
case "$1" in

    start)
        start
        ;;

    run)
        MOSAIC_BACKGROUND="no"
        start
        ;;

    debug)
        MOSAIC_DEBUG_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=${MOSAIC_DEBUG_PORT}"
        start
        ;;

    status)
        status
        ;;

    stop)
        stop
        ;;

    restart)
        stop
        start
        ;;

    *)
        echo "Usage: $0 {start|debug|stop|restart|status}"
        EXIT_CODE=1

esac
exit ${EXIT_CODE}

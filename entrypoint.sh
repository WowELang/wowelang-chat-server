#!/bin/sh
# This script will be the entrypoint for the Docker container.
# It ensures that environment variables like JAVA_HOME are correctly resolved
# when setting Java system properties.

# JAVA_HOME should be set by the base openjdk:21-slim image.
# The cacerts file was modified during the Docker build at $JAVA_HOME/lib/security/cacerts

echo "--- Entrypoint Script Start ---"
echo "JAVA_HOME: ${JAVA_HOME}"
TRUSTSTORE_FILE="${JAVA_HOME}/lib/security/cacerts"
echo "Truststore file path: ${TRUSTSTORE_FILE}"

if [ -f "${TRUSTSTORE_FILE}" ]; then
    echo "Truststore file exists."
    ls -l "${TRUSTSTORE_FILE}"
else
    echo "WARNING: Truststore file does not exist at ${TRUSTSTORE_FILE}!"
fi
echo "--- Starting Java Application ---"

# Execute the Java application
exec java \
    -Djavax.net.ssl.trustStore="${TRUSTSTORE_FILE}" \
    -Djavax.net.ssl.trustStorePassword=changeit \
    -jar /app.jar

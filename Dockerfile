FROM openjdk:21-slim

# 필요한 패키지 설치
RUN apt-get update && apt-get install -y wget ca-certificates && rm -rf /var/lib/apt/lists/*

# DocumentDB CA 인증서 다운로드 및 설정
RUN echo "JAVA_HOME is: $JAVA_HOME" \
    && echo "Default cacerts path is: $JAVA_HOME/lib/security/cacerts" \
    && ls -l "$JAVA_HOME/lib/security/cacerts" \
    && wget -O /tmp/rds-combined-ca-bundle.pem https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem \
    && echo "Importing CA certificate..." \
    && keytool -import -alias rds-root -keystore "$JAVA_HOME/lib/security/cacerts" -file /tmp/rds-combined-ca-bundle.pem -storepass changeit -noprompt \
    && echo "CA certificate import finished. Verifying..." \
    && keytool -list -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit -alias rds-root \
    && rm /tmp/rds-combined-ca-bundle.pem \
    && echo "Temporary CA file removed."

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
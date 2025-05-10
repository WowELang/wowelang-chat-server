FROM openjdk:21

# DocumentDB CA 인증서 다운로드 및 설정
RUN wget -O /tmp/rds-combined-ca-bundle.pem https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem \
    && keytool -import -alias rds-root -keystore $JAVA_HOME/lib/security/cacerts -file /tmp/rds-combined-ca-bundle.pem -storepass changeit -noprompt \
    && rm /tmp/rds-combined-ca-bundle.pem

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
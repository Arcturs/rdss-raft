FROM maven:3.8.3-openjdk-17-slim as BUILDER
ARG VERSION=0.0.1-SNAPSHOT
WORKDIR /opt/build/db
COPY pom.xml /opt/build/db/
COPY src /opt/build/db/src/
RUN mvn -f /opt/build/db/pom.xml clean package -B -DskipTests


FROM openjdk:17-alpine
WORKDIR /opt/app/db
COPY --from=BUILDER /opt/build/db/target/*.jar /opt/app/db/db.jar

RUN apk --no-cache add curl

ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java","-jar", "/opt/app/db/db.jar"]
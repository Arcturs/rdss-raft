FROM maven:3.8.3-openjdk-17-slim as BUILDER
ARG VERSION=0.0.1-SNAPSHOT
WORKDIR /opt/build/helper
COPY pom.xml /opt/build/helper/
COPY src /opt/build/helper/src/
RUN mvn -f /opt/build/helper/pom.xml clean package -B -DskipTests


FROM openjdk:17-alpine
WORKDIR /opt/app/helper
COPY --from=BUILDER /opt/build/helper/target/*.jar /opt/app/helper/helper.jar

RUN apk --no-cache add curl

ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java","-jar", "/opt/app/helper/helper.jar"]
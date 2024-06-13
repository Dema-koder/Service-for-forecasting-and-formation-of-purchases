FROM openjdk:22-ea-28-jdk

WORKDIR /var

COPY /target/backend-1.0-SNAPSHOT.jar /var/backend.jar

RUN mkdir -p /var/log/project

CMD ["java", "-jar", "backend.jar"]

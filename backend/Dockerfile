FROM openjdk:22-ea-28-jdk

RUN apt-get update && apt-get install -y python3 python3-pip

WORKDIR /app

COPY . /app

COPY /target/backend-1.0-SNAPSHOT.jar /var/backend.jar

RUN mkdir -p /var/log/project

CMD ["java", "-jar", "backend.jar"]

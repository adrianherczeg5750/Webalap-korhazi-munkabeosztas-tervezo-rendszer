FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y python3 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw -B dependency:go-offline
COPY src src

EXPOSE 8080

CMD ["./mvnw", "quarkus:dev"]
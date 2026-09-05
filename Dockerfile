FROM gradle:9.5.0-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/install/dotbit-backend/ ./
ENV PORT=8080
EXPOSE 8080
CMD ["./bin/dotbit-backend"]

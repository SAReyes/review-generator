FROM gradle:4.9 AS builder
COPY --chown=gradle:gradle . /opt/reviews
WORKDIR /opt/reviews
RUN gradle build

FROM openjdk:8-jre
COPY --from=builder /opt/reviews/build/libs/*.jar /app/app.jar
ADD entrypoint.sh /app/entrypoint.sh
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh", "java", "-jar", "app.jar", "1"]

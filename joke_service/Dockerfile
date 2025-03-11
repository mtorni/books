FROM alpine/java:21-jdk
ADD target/joke_service-0.0.1-SNAPSHOT.jar joke_service.jar
ENTRYPOINT exec java -jar joke_service.jar
EXPOSE 8080
EXPOSE 8000
EXPOSE 80
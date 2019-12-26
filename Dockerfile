FROM gradle:4.10.3-jdk8 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build

FROM openjdk:8
COPY --from=builder /home/gradle/src/build/libs/kshare-0.1.jar /app/kshare-0.1.jar
WORKDIR /app
EXPOSE 90
CMD ["java", "-jar", "kshare-0.1.jar", "-ssl", "false", "-d", "kshare.me", "-port", "90"]
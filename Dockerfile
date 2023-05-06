FROM maven:3.9.1 AS build

COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

FROM openjdk:17

COPY --from=build /home/app/target/smg-telegram-bot.jar /usr/local/lib/smg-telegram-bot.jar

EXPOSE 8080
ENTRYPOINT ["java", \
    "-ea", "-Duser.country=RU", "-Duser.language=ru", \
    "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", \
    "-jar", "/usr/local/lib/smg-telegram-bot.jar"]

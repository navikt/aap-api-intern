# Bruker Chainguard secure base image, https://sikkerhet.nav.no/docs/sikker-utvikling/baseimages

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jdk:openjdk-25

WORKDIR /app
COPY /app/build/libs/app-all.jar /app/app.jar

ENV LANG='nb_NO.UTF-8' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -XX:ActiveProcessorCount=2"

CMD ["java", "-jar", "app.jar"]

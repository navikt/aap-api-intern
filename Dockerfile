# Bruker Chainguard secure base images, https://sikkerhet.nav.no/docs/sikker-utvikling/baseimages

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jdk:openjdk-21-dev as jre
# trenger jdk image for å få 'jlink'

USER root
# --strip-debug uses objcopy from binutils
RUN apk add binutils

# Build small JRE image
RUN jlink \
    --verbose \
    --module-path $JAVA_HOME/bin/jmods/ \
    --add-modules java.base,java.desktop,java.management,java.naming,java.net.http,java.security.jgss,java.security.sasl,java.sql,jdk.httpserver,jdk.unsupported,jdk.crypto.ec,java.instrument \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /customjre


FROM cgr.dev/chainguard/static:latest as app
ENV JAVA_HOME=/jre
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=jre /customjre $JAVA_HOME
COPY /app/build/libs/app-all.jar app.jar

CMD ["java", "-XX:ActiveProcessorCount=2", "-jar", "app.jar"]

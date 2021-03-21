# STAGE 1: Build the binary
FROM openlaw/scala-builder:node AS builder

WORKDIR /src
COPY . .
RUN apk add --update nodejs npm
RUN sbt -mem 2048 dist
RUN mkdir dist
RUN cd dist && unzip /src/app/jvm/target/universal/*.zip


# STAGE 2: Run the binary
FROM openjdk:11-jre

COPY --from=builder /src/dist/server-1.0 /app

WORKDIR /app

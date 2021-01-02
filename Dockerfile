FROM openlaw/scala-builder:node

COPY . .
RUN apk add --update nodejs npm
RUN sbt -mem 2048 dist
RUN mkdir dist
RUN cd dist && unzip /src/app/jvm/target/universal/*.zip

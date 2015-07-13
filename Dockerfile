FROM rwilsonltf/scala-base

COPY app /app

WORKDIR /app

RUN sbt clean stage

EXPOSE 8888

EXPOSE 9990

CMD ./target/universal/stage/bin/twitter-server-backend -J-Xmx2048M -J-Xms1024M
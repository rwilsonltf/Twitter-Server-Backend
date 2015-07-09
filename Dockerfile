FROM rwilsonltf/scala-base

COPY app /app

WORKDIR /app

RUN sbt compile

EXPOSE 8888

EXPOSE 9990

CMD sbt run

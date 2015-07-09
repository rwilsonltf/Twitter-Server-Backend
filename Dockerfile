FROM rwilsonltf/scala-base

RUN git clone https://github.com/rwilsonltf/Twitter-Server-Backend.git

WORKDIR Twitter-Server-Backend

RUN sbt compile

EXPOSE 8888

EXPOSE 9990

CMD sbt run

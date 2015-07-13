name := "Twitter-Server-Backend"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "twittr" at "http://maven.twttr.com/"

libraryDependencies ++= Seq(
  "com.twitter" %% "twitter-server" % "1.11.0"
)

enablePlugins(JavaAppPackaging)
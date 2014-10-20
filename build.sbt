name := """riksdagen"""

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.3"

libraryDependencies ++= Seq( 
    "com.typesafe.play" %% "play-ws" % "2.4.0-M1",
    "com.typesafe.play" %% "play-json" % "2.4.0-M1",
    "com.typesafe.akka" %% "akka-actor" % "2.3.6"
)


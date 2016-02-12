version := "1.0"

lazy val commonSettings = Seq(
  organization := "com.instantor",
  version      := "1.0",
  scalaVersion := "2.11.7"
)

val akkaVer = "2.4.1"

val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVer,
  "com.typesafe.akka" %% "akka-remote" % akkaVer
)

// ----- sub-projects -----

lazy val common = (project in file("common")).
  settings(commonSettings: _*).
  settings(
    name := "common"
  )

lazy val `war-room` = (project in file("server")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= akkaDeps
  ).
  dependsOn(common)

lazy val defender = (project in file("client")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= akkaDeps
  ).
  dependsOn(common)

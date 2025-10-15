// Build plugins required by build.sbt
// - sbt-assembly: for assemblyMergeStrategy and fat JAR builds
// - sbt-native-packager: for DockerPlugin and JavaAppPackaging

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.5")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")



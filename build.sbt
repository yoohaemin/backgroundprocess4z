inThisBuild(
  List(
    scalaVersion             := V.scala213,
    crossScalaVersions       := V.scalaAll,
    organization             := "com.yoohaemin",
    homepage                 := Some(url("https://github.com/yoohaemin/backgroundprocess4z")),
    licenses                 := List("MPL-2.0" -> new URL("https://www.mozilla.org/MPL/2.0/")),
    Test / parallelExecution := true,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/yoohaemin/backgroundprocess4z/"),
        "scm:git:git@github.com:yoohaemin/backgroundprocess4z.git"
      )
    ),
    developers := List(
      Developer(
        "yoohaemin",
        "Haemin Yoo",
        "haemin@zzz.pe.kr",
        url("https://github.com/yoohaemin")
      )
    ),
    ConsoleHelper.welcomeMessage,
    versionScheme    := Some("early-semver"),
    organizationName := "Haemin Yoo",
    startYear        := Some(2023)
  ) ::: ciSettings
)

name := "backgroundprocess4z"
addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias(
  "fmtCheck",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)

//TODO uncomment native crossbuilds when ZIO published against native 0.4.8+ is out
//Related: https://github.com/scala-native/scala-native/issues/2858
lazy val root = project
  .in(file("."))
  .aggregate(
    coreJVM,
    coreJS,
    catsEffectJVM,
    catsEffectJS,
    docs
  )
  .settings(
    crossScalaVersions := Nil
  )
  .enablePlugins(NoPublishPlugin)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(name := "backgroundprocess4z-core")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"          % V.zio,
      "dev.zio" %%% "zio-streams"  % V.zio,
      "dev.zio" %%% "zio-test"     % V.zio % Test,
      "dev.zio" %%% "zio-test-sbt" % V.zio % Test
    )
  )

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js

// Interop module with cats-effect
lazy val catsEffect = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("cats-effect"))
  .enablePlugins(BuildInfoPlugin)
  .settings(name := "backgroundprocess4z-cats-effect")
  .settings(commonSettings)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
      "scalaPartialVersion" -> CrossVersion.partialVersion(scalaVersion.value)
    ),
    buildInfoPackage := "backgroundprocess4z.catseffect",
    buildInfoObject  := "BuildInfo"
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % V.catsEffect
    )
  )
  .dependsOn(core)

lazy val catsEffectJVM = catsEffect.jvm
lazy val catsEffectJS  = catsEffect.js

///////////////////////// docs

lazy val jsdocs = project
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % V.scalajsDom,
    crossScalaVersions                     := List(V.scala213)
  )
  .dependsOn(coreJS)
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(NoPublishPlugin)

lazy val docs = project
  .in(file("mdoc"))
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    name       := "backgroundprocess4z-docs",
    moduleName := name.value,
    mdocIn     := (ThisBuild / baseDirectory).value / "mdoc" / "docs",
    mdocOut    := (ThisBuild / baseDirectory).value / "vuepress" / "docs",
    run / fork := false,
    scalacOptions -= "-Xfatal-warnings",
    mdocJS             := Some(jsdocs),
    crossScalaVersions := List(V.scala213),
    mdocVariables := Map(
      "SNAPSHOTVERSION" -> version.value,
      "RELEASEVERSION"  -> version.value.takeWhile(_ != '+')
    )
  )
  .dependsOn(coreJVM)
  .enablePlugins(NoPublishPlugin)

lazy val commonSettings = Def.settings(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:higherKinds",
    "-language:existentials",
    "-unchecked",
    "-Xfatal-warnings"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) =>
      Seq(
        "-Xsource:3",
        "-Xlint:-byname-implicit",
        "-explaintypes",
        "-Vimplicits",
        "-Vtype-diffs",
        "-P:kind-projector:underscore-placeholders"
      )
    case Some((3, _)) =>
      Seq(
        "-no-indent",
        "-Ykind-projector"
      )
    case _ => Nil
  }),
  Test / fork := false,
  run / fork  := true,
  libraryDependencies ++= {
    if (scalaVersion.value.startsWith("2.13"))
      List(compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full))
    else
      Nil
  }
)

lazy val V = new {
  val scala213 = "2.13.10"
  val scala3   = "3.2.1"
  val scalaAll = scala213 :: scala3 :: Nil

  val catsEffect = "3.4.5"
  val zio        = "2.0.6"
  val scalajsDom = "2.3.0"
}

lazy val ciSettings = List(
  githubWorkflowPublishTargetBranches := List(RefPredicate.Equals(Ref.Branch("master"))),
  githubWorkflowJavaVersions          := Seq(JavaSpec.temurin("8")),
  githubWorkflowUseSbtThinClient      := false,
  githubWorkflowBuild                 := Seq(WorkflowStep.Sbt(List("++${{ matrix.scala }} test"))),
  githubWorkflowPublishTargetBranches += RefPredicate.StartsWith(Ref.Tag("v")),
  githubWorkflowTargetTags ++= Seq("v*"),
  githubWorkflowPublish := Seq(
    WorkflowStep.Sbt(
      List("ci-release"),
      env = Map(
        "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
      )
    )
  ),
  githubWorkflowGeneratedUploadSteps := {
    val skipCache = List("fetch/.js", "jsdocs", "mdoc")

    githubWorkflowGeneratedUploadSteps.value match {
      case (run: WorkflowStep.Run) :: t if run.commands.head.startsWith("tar cf") =>
        assert(run.commands.length == 1)
        run.copy(
          commands = List(
            skipCache.foldLeft(run.commands.head) { (acc, v) =>
              acc.replace(s"$v/target", "")
            }
          )
        ) :: t
      case l => l
    }
  },
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository     := "https://s01.oss.sonatype.org/service/local"
)

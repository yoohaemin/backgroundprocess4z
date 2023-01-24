import sbt.Keys._

object ConsoleHelper {
  def prompt: String               = s"${Console.CYAN}>${Console.RESET} "
  def header(text: String): String = s"${Console.GREEN}$text${Console.RESET}"

  def item(text: String): String =
    s"${Console.RED}> ${Console.CYAN}$text${Console.RESET}"

  // format: off
  val welcomeMessage =
    onLoadMessage :=
      raw"""|${header(raw"""""")}
            |${header(raw"""Background Process for ZIO""")}
            |${header(raw"""${version.value}""")}
            |
            |Useful sbt tasks:
            |${item("~compile")} - Compile all modules with file-watch enabled
            |${item("+test")} - Run the unit test suite
            |${item("fmt")} - Run scalafmt on the entire project
            |${item("+publishLocal")} - Publish backgroundprocess4z locally""".stripMargin
  // format: on
}

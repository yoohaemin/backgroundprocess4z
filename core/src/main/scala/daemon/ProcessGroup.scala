package backgroundprocess4z

import zio.UIO

/**
 * A ProcessGroup is a set of BackgroundProcesses under the same prefix.
 * Whenever a value is generated from the `feed` stream, the `run` method is called.
 *
 * @tparam R Environment required to run a single process
 */
trait ProcessGroup[R] {

  /**
   * A stable identifier to uniquely identify this ProcessGroup
   */
  val prefix: ProcessGroup.Prefix

  /**
   *
   * @return
   */
  def processes: BackgroundProcess[R, ?]

  def subGroups: List[ProcessGroup[R]]

  def subGroup(prefix: ProcessGroup.Prefix): Option[ProcessGroup[R]]

  def register[RR <: R](process: BackgroundProcess[RR, ?]): UIO[ProcessGroup[RR]]

  def register[RR <: R](group: ProcessGroup[RR]): UIO[ProcessGroup[RR]]

  /**
   * @return the status of all processes that belongs to this group and subgroups
   */
  def status: UIO[ProcessGroup.Status]

  /**
   * @return
   */
  def start: UIO[StartedProcess[R]]

}

object ProcessGroup {
  final case class Prefix(value: String)
  object Prefix {
    val root: Prefix = Prefix("")
  }

  final case class Status(
    subgroups: Map[Prefix, Status],
    rootProcesses: Map[BackgroundProcess.Identifier, BackgroundProcess.Status[?]]
  )
}

package backgroundprocess4z

import zio.*


trait StartedProcess[R] {

  def get(identifier: BackgroundProcess.QualifiedIdentifier): Option[BackgroundProcess[R, ?]]

  final def status(identifier: BackgroundProcess.QualifiedIdentifier): UIO[Option[BackgroundProcess.Status[?]]] =
    ZIO.foreach(get(identifier))(_.status)

  def subgroups: List[ProcessGroup[R]]

  def rootProcesses: List[BackgroundProcess[R, ?]]

  def statusAll: UIO[ProcessGroup.Status] =
    for {
      group   <- ZIO.foreach(subgroups)(g => g.status.map(g.prefix -> _))
      process <- ZIO.foreach(rootProcesses)(p => p.status.map(p.identifier -> _))
    } yield ProcessGroup.Status(subgroups = group.toMap, rootProcesses = process.toMap)

}

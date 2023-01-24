/*
 * Copyright (c) 2023 Haemin Yoo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package backgroundprocess4z

import zio.*
import zio.stream.ZStream

import java.time.Instant
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * A BackgroundProcess is a thin datatype around two effectful values: `feed` and `run`.
 * Whenever a value is generated from the `feed` stream, the `run` method is called.
 *
 * @tparam R Environment required to run `run` or `feed` effects
 * @tparam E Errors that can happen during `run`
 */
trait BackgroundProcess[-R, E] {

  /**
   * A stable identifier to uniquely identify this background process
   */
  val identifier: BackgroundProcess.Identifier

  /**
   * A message that gets generated from the feed, that triggers running the specified effect.
   */
  type FeedElement

  /**
   * A stream that triggers the `run` effect.
   */
  def feed: ZStream[R, Nothing, FeedElement]

  /**
   * The effect that runs whenever the feed produces a value.
   *
   * @param wakeup Value produced by the `feed` stream.
   */
  def run(wakeup: FeedElement): ZIO[R & Clock, E, ?]

  /**
   * The result of the most recent invocation of `run`.
   * This call will block if `run` never happened.
   */
  def status: UIO[BackgroundProcess.Status[E]]

  /**
   * The result of all of the invocations of `run`
   */
  def history: UIO[List[BackgroundProcess.Status[E]]]

  /**
   * Transforms an error value into a human readable String.
   */
  protected def formatError(e: E): String

}

object BackgroundProcess {
  final case class Identifier(value: String)

  final case class QualifiedIdentifier(
    group: List[ProcessGroup.Prefix],
    process: Identifier
  )

  trait Status[+E] {
    def feedElement: Any
    def failure: Option[E]
    def start: Instant
    def duration: Duration

    def isSuccessful: Boolean = failure.isEmpty
    def isFailed: Boolean     = !isSuccessful

    def formatted: String
  }

  private final class BackgroundProcessImpl[-R, E, FeedElem](
    override val identifier: Identifier,
    override val feed: ZStream[R, Nothing, FeedElem],
    run: FeedElem => ZIO[R, E, ?],
    formatError: E => String = _.toString
  )(
    statusHistory: Ref[mutable.ArrayDeque[Status[E]]],
    statusExists: Promise[Nothing, Unit],
    lastAccessedStatusList: Ref[(Status[E], List[Status[E]])]
  ) extends BackgroundProcess[R, E] {

    mutable.ArrayDeque

    override type FeedElement = FeedElem

    override def run(wakeup: FeedElement): ZIO[R & Clock, E, ?] =
      for {
        result <- run.apply(wakeup).either
        _      <- result.fold()
      } yield ()

    override val status: UIO[Status[E]] =
      statusExists.await *> (for {
        history <- statusHistory.get
        idx     <- statusHistoryCurrentIndex.get
      } yield history(idx))

    override val history: UIO[List[Status[E]]] =
      statusExists.await *> (for {
        history <- statusHistory.get
        idx     <- statusHistoryCurrentIndex.get
        cache   <- lastAccessedStatusList.get
        cacheHit = cache._1 == history(idx)
        result <-
          if (cacheHit) ZIO.succeed(cache._2)
          else {
            val old =
              if (idx + 1 < history.length) history.slice(idx + 1, history.length) else Array.empty
            val recent = history.slice(0, idx + 1)
            val result = ListBuffer
              .empty[Status[E]]
              .addAll(old.filterNot(_ eq null))
              .addAll(recent.filterNot(_ eq null))
              .result()
            lastAccessedStatusList.set(result.head -> result).as(result)
          }
      } yield result)

    override protected def formatError(e: E): String = formatError.apply(e)
  }

}

/*
val process = new BackgroundProcess[State, FooError] {
  val identifier = "Blah"
  val run = ???
  val startEvery = 1.minute
  val recoverPolicy = // Retry(schedule), Ignore, Halt, ...
  val report = (E, State) => UIO[Unit]
}
 */

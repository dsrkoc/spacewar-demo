package spacewar.client

import akka.actor.{Actor, ActorSelection}
import spacewar.messages.{GameOver, RegisterListener}

/** Listens for messages from the server side.
  * Used for messages that server dispatches to general audience.
  *
  * Created by dsrkoc on 2016-02-19.
  */
class Listener(supervisor: ActorSelection) extends Actor {

  override def receive = {
    case RegisterListener =>
      supervisor ! RegisterListener
      context become listenerMode
  }

  def listenerMode: Receive = {
    case msg: String => println(msg)

    case GameOver(winner) =>
      println(s"Game is over. The winning alliance is '$winner'.")
      context.system.terminate()
  }
}

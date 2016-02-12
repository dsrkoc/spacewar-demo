import akka.actor._
import spacewar.server.Supervisor

object WarRoom extends App {

  val system = ActorSystem("WarRoom")
  system.actorOf(Props[Supervisor], "Supervisor")
}

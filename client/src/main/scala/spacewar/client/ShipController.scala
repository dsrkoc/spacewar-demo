package spacewar.client

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import spacewar.equipment.Ship
import spacewar.messages.{RegisterListener, Fire, Register}

/**
  * Created by dsrkoc on 2016-02-11.
  */
class ShipController private (ship: Ship, name: String) {
  import ShipController._

  private val comm = system.actorOf(shipCommProps)
  comm ! Register(ship.armour, ship.agility, name)

  def fire(): Unit = comm ! Fire("", ship.firepower, ship.accuracy, None)
}
// todo ideas: make mining ships (mining for resources, cannot fight)

object ShipController {
  private val cfg = ConfigFactory.load().getConfig("war-room")

  private val system = ActorSystem("ShipController")
  private val supervisor = system actorSelection s"akka.tcp://WarRoom@${cfg.getString("host")}:${cfg.getInt("port")}/user/Supervisor"

  system.actorOf(Props(classOf[Listener], supervisor)) ! RegisterListener

  private def shipCommProps = Props(classOf[ShipComm], supervisor)

  def registerShip(ship: Ship, name: String) = new ShipController(ship, name)
}

// ----------- bunch of ships make a fleet -----------

class FleetController private (name: String, fleet: Seq[Ship]) {
  private val controllers = fleet.zipWithIndex.map { case (ship, idx) =>
    ShipController.registerShip(ship, s"$name#$idx")
  }

  def fire(): Unit = controllers.foreach(_.fire())
}

object FleetController {
  def registerFleet(fleetName: String, ships: Ship*) = new FleetController(fleetName, ships)
}
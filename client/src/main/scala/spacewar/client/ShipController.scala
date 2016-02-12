package spacewar.client

import akka.actor.{ActorSystem, Props}
import spacewar.equipment.Ship
import spacewar.messages.{Fire, Register}

/**
  * Created by dsrkoc on 2016-02-11.
  */
class ShipController private (system: ActorSystem, ship: Ship, name: String) {
  private val comm = system.actorOf(Props[ShipComm])
  comm ! Register(ship.armour, ship.agility, name)

  def fire(): Unit = comm ! Fire("", ship.firepower, ship.accuracy, None)
}

object ShipController {
  val system = ActorSystem("ShipController")

  def registerShip(ship: Ship, name: String) = new ShipController(system, ship, name)
}
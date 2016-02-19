package spacewar.client

import akka.actor.{Actor, ActorSelection}
import spacewar.alliances.Alliance
import spacewar.messages._

import scala.collection.immutable.Queue

/** Acts as communication channel between a ship and server side war supervisor.
  * Includes some control logic such as controlling the rapidity of fire.
  *
  * Created by dsrkoc on 2016-02-11.
  */
class ShipComm(supervisor: ActorSelection) extends Actor {
  var alliance: Alliance = _
  var name: String = _

  var fireQueue = Queue.empty[Long] // a queue of fire request timestamps
  val maxFireQueueEntries = 5       // keep at most this much entries in the fireQueue
  val headDissipationRate = 5000L   // weapon can discharge if fire timestamp + this is less than first entry in fireQueue

  def receive = {
    case reg @ Register(_, _, shipName) =>
      name = shipName
      supervisor ! reg

    case RegisterResponse(myAlliance) =>
      alliance = myAlliance
      println(s"Your ship ($name) is registered as part of '$myAlliance' alliance")
      context become gameIsOn
  }

  def gameIsOn: Receive = {
    case fire: Fire =>
      if (isWeaponOverheated(System.currentTimeMillis()))
        println("The weapon is overheated! Withhold fire until it cools off.")
      else
        supervisor ! fire.copy(shipName = name, alliance = Some(alliance))

    case FireResponse(damage, shipName, isDestroyed) =>
      println(s"You hit '$shipName' with $damage damage.${ if (isDestroyed) " The ship is destroyed." else "" }")

    case ShipDamaged(armourLeft) =>
      println(s"Your ship ($name) has been hit. The armour is down to $armourLeft.")

    case ShipDestroyed =>
      println(s"Your ship ($name) is destroyed!")
      context become destroyed
  }

  def destroyed: Receive = {
    case _: Fire => // silently ignored println("Mr Aldridge I put it to you that you are dead.")
  }

  private def isWeaponOverheated(ts: Long): Boolean = {
    fireQueue = fireQueue enqueue ts

    if (fireQueue.length > maxFireQueueEntries) {
      val (tsFirst, q2) = fireQueue.dequeue
      fireQueue = q2
      ts - tsFirst < headDissipationRate
    } else false
  }
}

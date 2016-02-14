package spacewar.client

import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import spacewar.alliances.Alliance
import spacewar.messages._

import scala.collection.immutable.Queue

/**
  * Created by dsrkoc on 2016-02-11.
  */
class ShipComm extends Actor {
  val cfg = ConfigFactory.load().getConfig("war-room")
  val supervisor = context.actorSelection(s"akka.tcp://WarRoom@${cfg.getString("host")}:${cfg.getInt("port")}/user/Supervisor")

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
      println(s"Your ship has been hit. The armour is down to $armourLeft.")

    case ShipDestroyed =>
      println("Your ship is destroyed!")
      context become destroyed

    case GameOver(winner) =>
      println(s"Game is over. The winning alliance is '$winner'.")
      context stop self

    case msg: String => println(msg)
  }

  def destroyed: Receive = {
    case GameOver(winner) =>
      println(s"Game is over. The winning alliance is '$winner'.")
      context stop self

    case msg: String => println(msg)

    case _: Fire => println("Mr Aldridge I put it to you that you are dead.")
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

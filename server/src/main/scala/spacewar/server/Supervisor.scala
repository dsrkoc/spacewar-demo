package spacewar.server

import akka.actor._
import spacewar.alliances._
import spacewar.messages._

import scala.collection.mutable

class Supervisor extends Actor {
  private var allianceFlip = false
  private val deathPhrases = List("%s is pushing up daisies", "%s has met their fate", "%s is six feet under",
    "I put it to you that %s is dead", "%s went toes up")

  private case class ShipInfo(armour: Int, agility: Float, name: String, remoteActor: ActorRef)

  private val armies: mutable.Map[Alliance, List[ShipInfo]] = mutable.Map(Human -> List.empty, Tauran -> List.empty)
  private var receivers: List[ActorRef] = List.empty

  def receive = {
    /* Ships should register themselves before they can participate in the battle.
     * They are assigned to an alliance and stored, along with their ActorRef
     */
    case Register(armour, agility, name) =>
      val alliance = chooseAlliance
      armies += alliance -> (ShipInfo(armour, agility, name, sender) :: armies(alliance))
      receivers = sender :: receivers
      sender ! RegisterResponse(alliance)

    case Fire(firepower, accuracy, enemyOpt) =>
      val enemy = enemyOpt.get // of course there will be a value here ;-)
      val ships = armies(enemy)
      val idx = util.Random.nextInt(ships.size)
      val ship = ships(idx)

      val damage = calcDamage(firepower, accuracy, ship.agility)
      val remainingShield = ship.armour - damage

      if (remainingShield > 0) {
        armies(enemy) = ships.updated(idx, ship.copy(armour = remainingShield))
        sender ! FireResponse(damage, ship.name, enemyDestroyed = false)
        ship.remoteActor ! ShipDamaged(remainingShield)
      } else {
        armies(enemy) = ships.filterNot(_ == ship)
        sender ! FireResponse(damage, ship.name, enemyDestroyed = true)
        ship.remoteActor ! ShipDestroyed
        broadcast(pickDeathPhrase format ship.name)

        if (armies(enemy).isEmpty) { // all the enemies have been terminated
          broadcast(GameOver(enemy.other))
          context stop self
        }
      }

  }

  override def postStop(): Unit = println("=== Supervisor has stopped ===")

  private def broadcast(msg: AnyRef): Unit = receivers.foreach(_ ! msg)

  private def calcDamage(firepower: Int, accuracy: Float, targetAgility: Float): Int =
    (firepower * accuracy * (1 - targetAgility)).toInt

  private def chooseAlliance: Alliance = {
    allianceFlip = !allianceFlip
    if (allianceFlip) Human else Tauran
  }

  private def pickDeathPhrase = deathPhrases(util.Random.nextInt(deathPhrases.size))
}
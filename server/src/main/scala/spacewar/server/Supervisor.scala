package spacewar.server

import akka.actor._
import spacewar.alliances._
import spacewar.messages._

import scala.collection.mutable

class Supervisor extends Actor {
  private var allianceFlip = false
  private val deathPhrases = List("'%s' is pushing up daisies", "'%s' has met their fate", "'%s' is six feet under",
    "'%s' went toes up", "'%s' is as dead as a doornail", "'%' has bitten the dust", "'%s' has put on the wooden overcoat",
    "'%s' has assumed room temperature", "'%s' has met a sticky end", "'%s' is counting worms", "'%s' is as dead as a dodo",
    "'%s' has gone home in a box", "'%s' met their maker", "'%s' sleeps with the fishes")
  private val fleetNameRE = """(\w+)#\d+$""".r

  private case class ShipInfo(armour: Int, agility: Float, name: String, remoteActor: ActorRef)

  private val armies: mutable.Map[Alliance, List[ShipInfo]] = mutable.Map(Human -> List.empty, Tauran -> List.empty)
  private var receivers: List[ActorRef] = List.empty

  def receive = {
    /* Ships should register themselves before they can participate in the battle.
     * They are assigned to an alliance and stored, along with their ActorRef
     */
    case Register(armour, agility, name) =>
      val alliance = chooseAlliance(name)
      armies += alliance -> (ShipInfo(armour, agility, name, sender) :: armies(alliance))
      println(s"$name is registered for '$alliance' alliance, which now counts ${armies(alliance).size} ships.")
      sender ! RegisterResponse(alliance)

    // registering the receivers of general messages
    case RegisterListener => receivers = sender :: receivers

    case Fire(attackerName, firepower, accuracy, alliance) =>
      val enemy = alliance.get.other // of course there will be a value here in Option ;-)
      val ships = armies(enemy)
      val idx = util.Random.nextInt(ships.size)
      val ship = ships(idx)

      val damage = calcDamage(firepower, accuracy, ship.agility)
      val remainingShield = ship.armour - damage

      println(s"'$attackerName' inflicted $damage damage to '${ship.name}' (remaining shield: $remainingShield).")
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
          context.system.terminate() // shut down the whole system - ok b/c this is the only actor
        }
      }

  }

  override def postStop(): Unit = println("=== Supervisor has stopped ===")

  private def broadcast(msg: AnyRef): Unit = receivers.foreach(_ ! msg)

  private def calcDamage(firepower: Int, accuracy: Float, targetAgility: Float): Int = {
    val damage = (firepower * accuracy * (1 - targetAgility)).toInt
    val luck   = damage / 4
    damage + util.Random.nextInt(luck + 1) - luck / 2 // damage +/- 12%
  }

  private def chooseAlliance(name: String): Alliance = {
    fleetAlliance(name).getOrElse {
      allianceFlip = !allianceFlip
      if (allianceFlip) Human else Tauran
    }
  }

  private def pickDeathPhrase = deathPhrases(util.Random.nextInt(deathPhrases.size))

  // ship can be standalone or part of a fleet
  // in which case its name is formatted as "name#x", where `x` is an ordinal number
  private def fleetAlliance(name: String): Option[Alliance] = {
    (name match {
      case fleetNameRE(n) => Some(n)
      case _              => None
    }).flatMap { fleetName =>
      armies.find { case (alliance, ships) =>
        ships.exists(_.name matches (fleetName + """#\d+$"""))
      }.map(_._1)
    }
  }
}

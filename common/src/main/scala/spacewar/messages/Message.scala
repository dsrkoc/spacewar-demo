package spacewar.messages

import spacewar.alliances._

sealed trait Message extends Serializable

case class Register(armour: Int, agility: Float, shipName: String) extends Message
case class RegisterResponse(alliance: Alliance) extends Message

case class Fire(shipName: String, firepower: Int, accuracy: Float, alliance: Option[Alliance]) extends Message
case class FireResponse(damage: Int, shipName: String, enemyDestroyed: Boolean) extends Message
case class ShipDamaged(armourLeft: Int)
case object ShipDestroyed extends Message

case class SustainedDamage(damage: Int) extends Message

case class GameOver(winner: Alliance) extends Message
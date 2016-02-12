package spacewar.equipment

trait Equipment {
  def firepower: Int = 0
  def armour: Int = 0
  def agility: Float = 0.0F
  def accuracy: Float = 0.0F
}

trait BasePackage extends Equipment {
  override def firepower = 20
  override def armour = 100
  override def agility = 0.6F
  override def accuracy = 0.6F
}

trait FusionEngine extends BasePackage {
  override def agility = super.agility + (1 - super.agility) * 0.3F
  override def accuracy = super.accuracy * 0.9F
}

trait GravityEngine extends BasePackage {
  override def agility = super.agility + (1 - super.agility) * 0.5F
}

trait ImpossibleDrive extends BasePackage {
  override def agility = super.agility + (1 - super.agility) * 0.9F
  override def accuracy = super.accuracy * 0.4F
}

trait ParticleGun extends BasePackage {
  override def firepower = super.firepower + 25
  override def agility = super.agility * 0.9F
}

trait Lasers extends BasePackage {
  override def firepower = super.firepower + 30
  override def agility = super.agility * 0.75F
}

trait EnergyShield extends BasePackage {
  override def armour = super.armour + 40
  override def accuracy = super.accuracy * 0.9F
}

trait CompositeArmour extends BasePackage {
  override def armour = super.armour + 25
  override def agility = super.agility * 0.8F
}
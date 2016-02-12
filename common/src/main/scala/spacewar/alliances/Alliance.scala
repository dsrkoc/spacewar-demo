package spacewar.alliances

sealed trait Alliance {
  def other: Alliance
}

case object Human extends Alliance {
  override val other = Tauran
  override def toString = "United Nations Exploratory Force"
}

case object Tauran extends Alliance {
  override val other = Human
  override def toString = "The Taurans"
}

sealed trait Resultado[A] {

  def esExito: Boolean = this match
    case Exito(_)  => true
    case Fallo(_)  => false

  def mapear[B](f: A => B): Resultado[B] = this match
    case Exito(valor)  => Exito(f(valor))
    case Fallo(motivo) => Fallo(motivo)

  def encadenar[B](f: A => Resultado[B]): Resultado[B] = this match
    case Exito(valor)  => f(valor)
    case Fallo(motivo) => Fallo(motivo)
}

case class Exito[A](valor: A) extends Resultado[A]
case class Fallo[A](motivo: String) extends Resultado[A]

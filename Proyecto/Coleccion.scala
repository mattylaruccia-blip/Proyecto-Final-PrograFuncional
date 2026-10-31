case class Coleccion[A](elementos: List[A] = Nil) {
  def agregar(a: A): Coleccion[A] = copy(elementos :+ a)

  def buscar(criterio: A => Boolean): List[A] = elementos.filter(criterio)

  def eliminarDonde(criterio: A => Boolean): Coleccion[A] =
    copy(elementos.filterNot(criterio))

  def actualizarDonde(criterio: A => Boolean)(cambio: A => A): Coleccion[A] =
    copy(elementos.map(a => if criterio(a) then cambio(a) else a))

  def contarPor[K](clave: A => K): Map[K, Int] =
    elementos.groupBy(clave).map((k, v) => (k, v.size))
}
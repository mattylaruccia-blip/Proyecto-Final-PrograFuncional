object ReglasPuntaje {
  // (puntos equipo1, puntos equipo2) según el resultado de la partida
  val clasica: Estado => (Int, Int) = {
    case FinalizadoGanoEquipo1 => (3, 0)
    case FinalizadoGanoEquipo2 => (0, 3)
    case FinalizadoEmpate      => (1, 1)
    case _                     => (0, 0)
  }
}

object Utilidades {

  // ---------- Genéricas con foldLeft ----------
  def longitud[A](items: List[A]): Int =
    items.foldLeft(0)((acc, _) => acc + 1)

  def contar[A](items: List[A])(condicion: A => Boolean): Int =
    items.foldLeft(0)((acc, x) => if condicion(x) then acc + 1 else acc)

  def ultimo[A](items: List[A]): Option[A] =
    items.foldLeft(None: Option[A])((_, x) => Some(x))

  // ---------- Genérica con reduce + filter ----------
  // Los n mejores según una función de puntaje; no falla si hay menos de n elementos
  def top[A](items: List[A], n: Int)(puntaje: A => Int): List[A] = items match
    case Nil => Nil
    case _ =>
      if n <= 0 then Nil
      else {
        val mayor = items.reduce((a, b) => if puntaje(a) >= puntaje(b) then a else b)
        mayor :: top(items.filter(x => x != mayor), n - 1)(puntaje)
      }

  // ---------- Orden superior: la regla de puntaje es un parámetro ----------
  def calcularTabla(equipos: List[Equipo], partidas: List[PartidaPuntos])
                   (regla: Estado => (Int, Int)): List[ResultadoEquipo] =
    partidas.foldLeft(equipos.map(e => ResultadoEquipo(e, 0))) { (tabla, p) =>
      val (pts1, pts2) = regla(p.estado)
      tabla.map(r =>
        if r.equipo == p.equipo1 then r.copy(puntosTotales = r.puntosTotales + pts1)
        else if r.equipo == p.equipo2 then r.copy(puntosTotales = r.puntosTotales + pts2)
        else r
      )
    }

  // ---------- Apoyo para los resúmenes ----------
  def ganadorFinal(partidas: List[Partida]): Option[Equipo] = ultimo(partidas) match
    case Some(p) => p.ganador
    case None    => None

  def perdedorFinal(partidas: List[Partida]): Option[Equipo] = ultimo(partidas) match
    case Some(p) => p.perdedor
    case None    => None

  def nombreDe(equipo: Option[Equipo]): String = equipo match
    case Some(e) => e.nombreEquipo
    case None    => "Sin definir"

  def formatearPodio(podio: List[ResultadoEquipo], posicion: Int = 1): String = podio match
    case Nil => ""
    case cabeza :: cola =>
      s"$posicion. ${cabeza.descripcionResultado()}\n${formatearPodio(cola, posicion + 1)}"
}
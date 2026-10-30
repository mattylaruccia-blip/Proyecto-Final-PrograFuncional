case class PartidaPuntos(
    val puntaje1: Int,
    val puntaje2: Int,
    override val idPartida: String,
    override val equipo1: Equipo,
    override val equipo2: Equipo,
    override val estado: Estado
) extends Partida(idPartida, equipo1, equipo2, estado) {
    def resumen(): String = {
        estado match
            case FinalizadoGanoEquipo1 => s"$estado.descripcion(): ${equipo1.nombreEquipo} con $puntaje1 puntos"
            case FinalizadoGanoEquipo2 => s"$estado.descripcion(): ${equipo2.nombreEquipo} con $puntaje2 puntos"
            case FinalizadoEmpate => s"${estado.descripcion()}"
            case _ => "La partida aún no ha finalizado"
    }

    //def actualizarPuntaje

    def actualizarEstadoPartida(estadoNuevo: Estado): Partida = copy(estado = if puntaje1 > puntaje2 then FinalizadoGanoEquipo1 else if puntaje2 > puntaje1 then FinalizadoGanoEquipo2 else FinalizadoEmpate)
}
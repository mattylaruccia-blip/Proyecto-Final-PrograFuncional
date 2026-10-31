case class TorneoDobleEliminacion(
    val cuadroGanadores: TorneoEliminacionDirecta,
    val cuadroPerdedores: TorneoEliminacionDirecta,
    override val idTorneo: Int,
    override val nombreTorneo: String,
    override val videojuego: Videojuego,
    override val modalidad: ModalidadTorneo,
    override val equipos: List[Equipo],
    override val estado: Estado
) extends Torneo(idTorneo, nombreTorneo, videojuego, modalidad, equipos, estado) {
    def descripción(): String = s"Torneo de eliminación doble\nID: $idTorneo\nNombre: $nombreTorneo\nJuego: ${videojuego.titulo}\nModalidad: ${modalidad.descripcion()}\nEstado: ${estado.descripcion()}"
    def actualizarEstadoTorneo(estadoNuevo: Estado): Torneo = copy(estado = estadoNuevo)

    protected def detalleFinal(partidas: List[Partida]): String = {
        val campeon    = Utilidades.ganadorFinal(partidas)   // la gran final es la última partida
        val subcampeon = Utilidades.perdedorFinal(partidas)
        val jugadas    = Utilidades.contar(partidas)(_.finalizada)

        val trayectoria = campeon match
            case None => "Trayectoria no determinada"
            case Some(c) => val derrotas = Utilidades.contar(partidas)(_.perdedor == Some(c))
                if derrotas == 0 then "Campeón invicto"
                else s"Campeón con $derrotas derrota(s) en el camino"

        s"Campeón: ${Utilidades.nombreDe(campeon)} ($trayectoria)\n" +
        s"Subcampeón: ${Utilidades.nombreDe(subcampeon)}\n" +
        s"Partidas jugadas: $jugadas"
    }
}
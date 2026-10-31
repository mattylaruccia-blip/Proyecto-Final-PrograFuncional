case class TorneoEliminacionDirecta(
    override val idTorneo: Int,
    override val nombreTorneo: String,
    override val videojuego: Videojuego,
    override val modalidad: ModalidadTorneo,
    override val equipos: List[Equipo],
    override val estado: Estado
) extends Torneo(idTorneo, nombreTorneo, videojuego, modalidad, equipos, estado) {
    def descripción(): String = s"Torneo de eliminación directa\nID: $idTorneo\nNombre: $nombreTorneo\nJuego: ${videojuego.titulo}\nModalidad: ${modalidad.descripcion()}\nEstado: ${estado.descripcion()}"
    
    def actualizarEstadoTorneo(estadoNuevo: Estado): Torneo = copy(estado = estadoNuevo)

    protected def detalleFinal(partidas: List[Partida]): String = {
        val campeon    = Utilidades.ganadorFinal(partidas)   // la final es la última partida
        val subcampeon = Utilidades.perdedorFinal(partidas)
        val jugadas    = Utilidades.contar(partidas)(_.finalizada)

        s"Campeón: ${Utilidades.nombreDe(campeon)}\n" +
        s"Subcampeón: ${Utilidades.nombreDe(subcampeon)}\n" +
        s"Partidas jugadas: $jugadas"
}
}
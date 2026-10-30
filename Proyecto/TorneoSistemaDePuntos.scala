case class TorneoSistemaDePuntos(
    val pesoClasificaion: Int,
    val puntajePorBaja: Int,
    override val idTorneo: Int,
    override val nombreTorneo: String,
    override val videojuego: Videojuego,
    override val modalidad: ModalidadTorneo,
    override val equipos: List[Equipo],
    override val estado: Estado
) extends Torneo(idTorneo, nombreTorneo, videojuego, modalidad, equipos, estado) {
    def descripción(): String = s"Torneo por sistema de puntos\nID: $idTorneo\nNombre: $nombreTorneo\nJuego: ${videojuego.titulo}\nModalidad: ${modalidad.descripcion()}\nEstado: ${estado.descripcion()}"
    
    def actualizarEstadoTorneo(estadoNuevo: Estado): Torneo = copy(estado = estadoNuevo)
}
abstract class Torneo(
    val idTorneo: Int,
    val nombreTorneo: String,
    val videojuego: Videojuego,
    val modalidad: ModalidadTorneo,
    val equipos: List[Equipo],
    val estado: Estado
) {
    def descripción(): String

    def actualizarEstadoTorneo(estadoNuevo: Estado): Torneo
}
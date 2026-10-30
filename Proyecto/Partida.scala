abstract class Partida(
    val idPartida: String,
    val equipo1: Equipo,
    val equipo2: Equipo,
    val estado: Estado
) {
    def resumen(): String

    def actualizarEstadoPartida(estadoNuevo: Estado): Partida
}
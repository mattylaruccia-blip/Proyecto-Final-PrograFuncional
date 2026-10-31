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

    protected def detalleFinal(partidas: List[Partida]): String

    def resumenFinal(partidas: List[Partida]): String = 
        estado match
            case Finalizado => s"RESUMEN FINAL\n${descripción()}\nEquipos participantes: ${Utilidades.longitud(equipos)}\n${detalleFinal(partidas)}"
            case _ => s"El torneo '$nombreTorneo' aún no ha finalizado (${estado.descripcion()})"
}
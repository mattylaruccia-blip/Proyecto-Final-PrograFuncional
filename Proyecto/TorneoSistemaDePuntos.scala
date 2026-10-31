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

    protected def detalleFinal(partidas: List[Partida]): String = {
        val jugadas = partidas.flatMap {
            case p: PartidaPuntos => List(p)
            case _ => Nil
        }
        val tabla    = Utilidades.calcularTabla(equipos, jugadas)(ReglasPuntaje.clasica)
        val podio    = Utilidades.top(tabla, 3)(_.puntosTotales)
        val empates  = Utilidades.contar(jugadas)(_.estado == FinalizadoEmpate)
        val anotados = jugadas.foldLeft(0)((acc, p) => acc + p.puntaje1 + p.puntaje2)

        s"Podio:\n${Utilidades.formatearPodio(podio)}" +
        s"Partidas jugadas: ${Utilidades.longitud(jugadas)}\n" +
        s"Empates: $empates\n" +
        s"Puntos anotados en total: $anotados"
}
}
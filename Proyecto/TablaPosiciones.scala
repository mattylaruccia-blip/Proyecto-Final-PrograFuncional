case class TablaPosiciones(
    val torneo: Torneo,
    val equipos: List[ResultadoEquipo],
    val fechaFinalizacion: String
) {
    def definirPodio(acc: Int = 0, podio: List[ResultadoEquipo] = Nil, listaEquipos: List[ResultadoEquipo] = equipos): List[ResultadoEquipo] = {
        val mayor: ResultadoEquipo = listaEquipos.reduce((a, b) => if a.puntosTotales > b.puntosTotales then a else b)
        if acc > 2 then podio.reverse
        else definirPodio(acc + 1, mayor :: podio, listaEquipos.filter(x => !x.equals(mayor)))
    }

    def imprimirPodio(podio: List[ResultadoEquipo] = definirPodio()): String = {
        podio match
            case Nil => ""
            case cabeza :: cola => s"${cabeza.descripcionResultado()}\n${imprimirPodio(cola)}"        
    }
}

object a{
    def main(args: Array[String]): Unit = {
        val equipo1: Equipo = Equipo(1, "Triple t", List(Participante(1, "Tokkf", 1, "Diego", "nose@gmail.com")))
        val equipo2: Equipo = Equipo(2, "Enanos Verdes", List(Participante(2, "La Trucha", 2, "Matias", "A@gmail.com")))
        val equipo3: Equipo = Equipo(3, "AAAAA", List(Participante(3, "Discipulo de Burgoa", 3, "Jose", "Godsman@gmail.com")))
        val equipo4: Equipo = Equipo(4, ":(", List(Participante(4, "Triple T", 4, "Marco", "TungTung@gmail.com")))
        val torneo1: Torneo = TorneoEliminacionDirecta(1, "AAAAA", Videojuego(1, "Smash", "Peleas"), Individual, List(equipo1, equipo2, equipo3, equipo4), EnCurso)
        val resultado1: ResultadoEquipo = ResultadoEquipo(equipo1, 100)
        val resultado2: ResultadoEquipo = ResultadoEquipo(equipo2, 500)
        val resultado3: ResultadoEquipo = ResultadoEquipo(equipo3, 300)
        val resultado4: ResultadoEquipo = ResultadoEquipo(equipo4, 400)
        val tablaPosiciones1: TablaPosiciones = TablaPosiciones(torneo1, List(resultado1, resultado2, resultado3, resultado4), "Algún día")
        println(tablaPosiciones1.imprimirPodio())
    }
}
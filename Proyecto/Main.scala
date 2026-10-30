def añadirTorneoActivo(torneos: List[Torneo], torneoNuevo: Torneo): List[Torneo] = {
    (torneoNuevo :: torneos.reverse).reverse
}

def eliminarTorneosFinalizados(torneos: List[Torneo]): List[Torneo] = {
    torneos.filter(torneo => torneo.estado != Finalizado)
}

// def eliminarTorneosFinalizados(torneos: List[Torneo], nuevaLista: List[Torneo] = Nil): List[Torneo] = {
//     torneos match {
//         case Nil => nuevaLista.reverse
//         case cabeza :: cola =>
//             cabeza.estado match {
//                 case Finalizado => eliminarTorneosFinalizados(cola, nuevaLista)
//                 case _ => eliminarTorneosFinalizados(cola, cabeza :: nuevaLista)
//             }
//     }
// }

def listarTorneosActivos(torneos: List[Torneo]): String = {
    torneos match
        case Nil => ""
        case cabeza :: cola => s"${cabeza.descripción()}\n\n${{listarTorneosActivos(cola)}}"
}

object main{
    def main(args: Array[String]): Unit = {
        
        val torneos: List[Torneo] = Nil

        // val videojuego1: Videojuego = new Videojuego(1, "Smash", "Peleas")
        // val participante1: Participante = new Participante(1, "Tokkf", 1, "Diego", "aaa@gmail.com")
        // val equipo1: Equipo = new Equipo(4, "Triple t", List(participante1))
        // val torneo1: Torneo = new Torneo(1, "No se", videojuego1, PorEquipos, List(equipo1), Pendiente)

        // val videojuego2: Videojuego = new Videojuego(2, "Marvel Rivals", "Hero Shooter")
        // val participante2: Participante = new Participante(2, "LaTrucha", 2, "Matias", "bbb@gmail.com")
        // val equipo2: Equipo = new Equipo(4, "Enanos verdes", List(participante2))
        // val torneo2: Torneo = new Torneo(2, "Si se", videojuego2, PorEquipos, List(equipo2), EnCurso)

        // val videojuego3: Videojuego = new Videojuego(3, "Call Of Duty", "FPS")
        // val participante3: Participante = new Participante(3, "Leon Kennedy de Temu", 3, "Paolo", "ccc@gmail.com")
        // val equipo3: Equipo = new Equipo(4, "Gatitos feudales", List(participante3))
        // val torneo3: Torneo = new Torneo(3, "Tal vez se", videojuego3, PorEquipos, List(equipo3), EnCurso)

        // val nuevaLista1: List[Torneo] = añadirTorneoActivo(torneos, torneo1)
        // println(listarTorneosActivos(nuevaLista1))
        // val nuevaLista2: List[Torneo] = añadirTorneoActivo(nuevaLista1, torneo2)
        // println(listarTorneosActivos(nuevaLista2))
        // val nuevaLista3: List[Torneo] = añadirTorneoActivo(nuevaLista2, torneo3)
        // println(listarTorneosActivos(nuevaLista3))
    }
}
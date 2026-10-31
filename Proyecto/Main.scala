def añadirTorneoActivo(torneos: List[Torneo], torneoNuevo: Torneo): List[Torneo] = {
    (torneoNuevo :: torneos.reverse).reverse
}

def eliminarTorneosFinalizados(torneos: List[Torneo]): List[Torneo] = {
    torneos.filter(torneo => torneo.estado != Finalizado)
}

def listarTorneosActivos(torneos: List[Torneo]): String = {
    torneos match
        case Nil => ""
        case cabeza :: cola => s"${cabeza.descripción()}\n\n${{listarTorneosActivos(cola)}}"
}

def porEstado(e: Estado): Torneo => Boolean = _.estado == e
def porVideojuego(titulo: String): Torneo => Boolean = _.videojuego.titulo == titulo

def y[A](p: A => Boolean, q: A => Boolean): A => Boolean = a => p(a) && q(a)
def o[A](p: A => Boolean, q: A => Boolean): A => Boolean = a => p(a) || q(a)
def no[A](p: A => Boolean): A => Boolean = a => !p(a)


object main{
    def main(args: Array[String]): Unit = {
        val equipo1: Equipo = Equipo(1, "Triple t", List(Participante(1, "Tokkf", 1, "Diego", "nose@gmail.com")))
        val equipo2: Equipo = Equipo(2, "Enanos Verdes", List(Participante(2, "La Trucha", 2, "Matias", "A@gmail.com")))
        val equipo3: Equipo = Equipo(3, "AAAAA", List(Participante(3, "Discipulo de Burgoa", 3, "Jose", "Godsman@gmail.com")))
        val equipo4: Equipo = Equipo(4, ":(", List(Participante(4, "Triple T", 4, "Marco", "TungTung@gmail.com")))
        val torneo1: Torneo = TorneoEliminacionDirecta(1, "AAAAA", Videojuego(1, "Smash", "Peleas"), Individual, List(equipo1, equipo2, equipo3, equipo4), EnCurso)

        val torneos = Coleccion[Torneo]()
        val torneos2 = torneos.agregar(torneo1)
        println(torneos2.buscar(y(porEstado(EnCurso), porVideojuego("Smash"))))
    }
}
sealed trait Estado{
    def descripcion(): String
}

case object Pendiente extends Estado{
    def descripcion(): String = "Pendiente"
}

case object EnCurso extends Estado{
    def descripcion(): String = "En curso"
}

case object Finalizado extends Estado{
    def descripcion(): String = "Finalizado"
}

case object FinalizadoGanoEquipo1 extends Estado{
    def descripcion(): String = "Ganó el equipo 1"
}

case object FinalizadoGanoEquipo2 extends Estado{
    def descripcion(): String = "Ganó el equipo 2"
}

case object FinalizadoEmpate extends Estado{
    def descripcion(): String = "La partida terminó en empate"
}
sealed trait ModalidadTorneo{
    def descripcion(): String
}

case object Individual extends ModalidadTorneo{
    def descripcion(): String = "Individual"
}

case object PorEquipos extends ModalidadTorneo{
    def descripcion(): String = "Por equipos"
}
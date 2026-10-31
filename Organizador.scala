case class Organizador(
    val torneo: List[Torneo],
    override val id: Int,
    override val nombre: String,
    override val correo: String
) extends Usuario(id, nombre, correo) {
    
}
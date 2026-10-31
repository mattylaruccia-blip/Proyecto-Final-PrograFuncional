case class Participante(
    val idParticipante: Int,
    val nickname: String,
    override val id: Int,
    override val nombre: String,
    override val correo: String
) extends Usuario(id, nombre, correo) {

}
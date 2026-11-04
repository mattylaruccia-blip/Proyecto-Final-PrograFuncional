case class Organizador(
  val idsTorneos: List[Int],
  override val id: Int,
  override val nombre: String,
  override val correo: String
) extends Usuario(id, nombre, correo) {

  def esDuenoDe(idTorneo: Int): Boolean =
    idsTorneos.filter(i => i == idTorneo).nonEmpty

  def conTorneo(idTorneo: Int): Organizador =
    copy(idsTorneos = idsTorneos :+ idTorneo)

  def misTorneos(estado: EstadoSistema): List[Torneo] =
    estado.torneos.filter(t => esDuenoDe(t.idTorneo))

  def crearTorneo(estado: EstadoSistema, torneo: Torneo): Resultado[EstadoSistema] =
    Sistema.agregarTorneo(estado, torneo).mapear(nuevo =>
      nuevo.copy(usuarios = registrarComoDueno(nuevo.usuarios, torneo.idTorneo)))

  private def registrarComoDueno(usuarios: List[Usuario], idTorneo: Int): List[Usuario] =
    usuarios.map {
      case o: Organizador if o.id == id => o.conTorneo(idTorneo)
      case otro => otro
    }

  private def exigirDueno(idTorneo: Int): Resultado[Int] =
    if esDuenoDe(idTorneo) then Exito[Int](idTorneo)
    else Fallo[Int](s"No puedes administrar el torneo $idTorneo: lo creó otro organizador")

  def iniciarTorneo(estado: EstadoSistema, idTorneo: Int): Resultado[EstadoSistema] =
    exigirDueno(idTorneo).encadenar(_ => Sistema.cambiarEstadoTorneo(estado, idTorneo, TorneoEnCurso))

  def finalizarTorneo(estado: EstadoSistema, idTorneo: Int): Resultado[EstadoSistema] =
    exigirDueno(idTorneo).encadenar(_ => Sistema.cambiarEstadoTorneo(estado, idTorneo, TorneoFinalizado))
}

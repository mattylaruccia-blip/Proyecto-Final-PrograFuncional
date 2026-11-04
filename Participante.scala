case class Participante(
  val idParticipante: Int,
  val nickname: String,
  override val id: Int,
  override val nombre: String,
  override val correo: String
) extends Usuario(id, nombre, correo) {

  def perteneceA(equipo: Equipo): Boolean =
    equipo.integrantes.filter(i => i.idParticipante == idParticipante).nonEmpty

  def misEquipos(estado: EstadoSistema): List[Equipo] =
    estado.equipos.filter(e => perteneceA(e))

  def equipoEn(torneo: Torneo): List[Equipo] =
    torneo.equipos.filter(e => perteneceA(e))

  def torneosInscritos(estado: EstadoSistema): List[Torneo] =
    estado.torneos.filter(t => equipoEn(t).nonEmpty)

  def unirseConEquipo(estado: EstadoSistema, idTorneo: Int, idEquipo: Int): Resultado[EstadoSistema] =
    Sistema.buscarEquipo(estado, idEquipo).encadenar(equipo =>
      if perteneceA(equipo) then Sistema.inscribirEquipo(estado, idTorneo, idEquipo)
      else Fallo[EstadoSistema](s"No eres integrante del equipo ${equipo.nombreEquipo}"))

  def unirseSolo(estado: EstadoSistema, idTorneo: Int): Resultado[EstadoSistema] =
    misEquipos(estado).filter(e => e.integrantes.length == 1) match
      case equipo :: _ => unirseConEquipo(estado, idTorneo, equipo.idEquipo)
      case Nil => unirseConEquipoNuevo(estado, idTorneo, nickname, Nil)

  def unirseConEquipoNuevo(estado: EstadoSistema, idTorneo: Int, nombreEquipo: String,
                           idsCompaneros: List[Int]): Resultado[EstadoSistema] =
    if nombreEquipo.trim.isEmpty then Fallo[EstadoSistema]("El equipo necesita un nombre")
    else resolverCompaneros(estado, idsCompaneros.filter(i => i != idParticipante).distinct)
      .encadenar(companeros =>
        crearEquipoEInscribir(estado, idTorneo,
          Equipo(Sistema.siguienteId(estado.equipos, (e: Equipo) => e.idEquipo), nombreEquipo.trim, this :: companeros)))

  private def crearEquipoEInscribir(estado: EstadoSistema, idTorneo: Int, equipo: Equipo): Resultado[EstadoSistema] =
    Sistema.registrarEquipo(estado, equipo)
      .encadenar(conEquipo => Sistema.inscribirEquipo(conEquipo, idTorneo, equipo.idEquipo))

  private def resolverCompaneros(estado: EstadoSistema, ids: List[Int]): Resultado[List[Participante]] =
    ids.foldLeft(Exito[List[Participante]](Nil): Resultado[List[Participante]])((acc, idCompanero) =>
      acc.encadenar(lista => Sistema.buscarParticipante(estado, idCompanero).mapear(p => lista :+ p)))
}

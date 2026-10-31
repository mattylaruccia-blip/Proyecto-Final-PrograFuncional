// Fachada del sistema: la interfaz (consola o gráfica) solo debería llamar a estas funciones.
// Todas son puras: reciben un EstadoSistema y devuelven un Resultado o una lista nueva.
object Sistema {

  val estadoInicial: EstadoSistema = EstadoSistema(Nil, Nil, Nil)

  // ---------- Utilidades genéricas ----------

  def siguienteId[A](elementos: List[A], obtenerId: A => Int): Int =
    elementos.foldLeft(0)((maximo, e) => if obtenerId(e) > maximo then obtenerId(e) else maximo) + 1

  private def agregarSiNoExiste[A](lista: List[A], nuevo: A, obtenerId: A => Int, entidad: String): Resultado[List[A]] =
    if lista.filter(e => obtenerId(e) == obtenerId(nuevo)).isEmpty then Exito(lista :+ nuevo)
    else Fallo(s"Ya existe un registro de $entidad con id ${obtenerId(nuevo)}")

  // ---------- Registro ----------

  def registrarVideojuego(estado: EstadoSistema, videojuego: Videojuego): Resultado[EstadoSistema] =
    agregarSiNoExiste(estado.videojuegos, videojuego, (v: Videojuego) => v.idJuego, "videojuego")
      .mapear(lista => estado.copy(videojuegos = lista))

  def registrarEquipo(estado: EstadoSistema, equipo: Equipo): Resultado[EstadoSistema] =
    agregarSiNoExiste(estado.equipos, equipo, (e: Equipo) => e.idEquipo, "equipo")
      .mapear(lista => estado.copy(equipos = lista))

  // Valida la modalidad, que el videojuego esté registrado y que el id no se repita.
  def agregarTorneo(estado: EstadoSistema, torneo: Torneo): Resultado[EstadoSistema] =
    if estado.videojuegos.filter(v => v.idJuego == torneo.videojuego.idJuego).isEmpty then
      Fallo(s"El videojuego ${torneo.videojuego.titulo} no está registrado")
    else
      Validaciones.validarTorneo(torneo)
        .encadenar(valido => agregarSiNoExiste(estado.torneos, valido, (t: Torneo) => t.idTorneo, "torneo"))
        .mapear(lista => estado.copy(torneos = lista))

  // ---------- Búsquedas ----------

  def buscarTorneo(estado: EstadoSistema, idTorneo: Int): Resultado[Torneo] =
    estado.torneos.filter(t => t.idTorneo == idTorneo) match
      case torneo :: _ => Exito(torneo)
      case Nil         => Fallo(s"No existe el torneo con id $idTorneo")

  def buscarEquipo(estado: EstadoSistema, idEquipo: Int): Resultado[Equipo] =
    estado.equipos.filter(e => e.idEquipo == idEquipo) match
      case equipo :: _ => Exito(equipo)
      case Nil         => Fallo(s"No existe el equipo con id $idEquipo")

  private def reemplazarTorneo(estado: EstadoSistema, nuevo: Torneo): EstadoSistema =
    estado.copy(torneos = estado.torneos.map(t => if t.idTorneo == nuevo.idTorneo then nuevo else t))

  // ---------- Operaciones sobre torneos ----------

  private def validarInscripcion(torneo: Torneo, equipo: Equipo): Resultado[Torneo] =
    torneo.estado match
      case Pendiente =>
        if torneo.equipos.filter(e => e.idEquipo == equipo.idEquipo).isEmpty then
          Validaciones.validarEquipos(torneo.modalidad, List(equipo)).mapear(_ => torneo)
        else Fallo(s"El equipo ${equipo.nombreEquipo} ya está inscrito en el torneo")
      case _ => Fallo("Solo se pueden inscribir equipos en torneos pendientes")

  def inscribirEquipo(estado: EstadoSistema, idTorneo: Int, idEquipo: Int): Resultado[EstadoSistema] =
    buscarEquipo(estado, idEquipo).encadenar(equipo =>
      buscarTorneo(estado, idTorneo)
        .encadenar(torneo => validarInscripcion(torneo, equipo))
        .mapear(torneo => reemplazarTorneo(estado, torneo.conEquipos(torneo.equipos :+ equipo))))

  private def transicionValida(actual: EstadoTorneo, nuevo: EstadoTorneo): Boolean = (actual, nuevo) match
    case (TorneoPendiente, TorneoEnCurso)  => true
    case (TorneoEnCurso, TorneoFinalizado) => true
    case _                     => false

  def cambiarEstadoTorneo(estado: EstadoSistema, idTorneo: Int, nuevoEstado: EstadoTorneo): Resultado[EstadoSistema] =
    buscarTorneo(estado, idTorneo).encadenar(torneo =>
      if !transicionValida(torneo.estado, nuevoEstado) then
        Fallo(s"No se puede pasar de '${torneo.estado.descripcion()}' a '${nuevoEstado.descripcion()}'")
      else if nuevoEstado == EnCurso && torneo.equipos.length < 2 then
        Fallo("Se necesitan al menos 2 equipos para iniciar el torneo")
      else
        Exito(reemplazarTorneo(estado, torneo.actualizarEstadoTorneo(nuevoEstado))))

  // Solo para torneos por sistema de puntos.
  private def tablaDe(torneo: Torneo, rondas: List[List[DesempenoEquipo]], fecha: String): Resultado[TablaPosiciones] =
    torneo match
      case t: TorneoSistemaDePuntos =>
        t.calcularResultados(rondas).mapear(resultados => TablaPosiciones(t, resultados, fecha))
      case _ => Fallo("Este torneo no usa sistema de puntos")

  def calcularTablaPuntos(estado: EstadoSistema, idTorneo: Int, rondas: List[List[DesempenoEquipo]], fecha: String): Resultado[TablaPosiciones] =
    buscarTorneo(estado, idTorneo).encadenar(torneo => tablaDe(torneo, rondas, fecha))

  // ---------- Consultas para la interfaz ----------

  // Función de orden superior: el criterio lo decide quien llama.
  def filtrarTorneos(estado: EstadoSistema, criterio: Torneo => Boolean): List[Torneo] =
    estado.torneos.filter(criterio)

  def torneosActivos(estado: EstadoSistema): List[Torneo] =
    filtrarTorneos(estado, t => t.estado != TorneoFinalizado)

  def torneosPorVideojuego(estado: EstadoSistema, idJuego: Int): List[Torneo] =
    filtrarTorneos(estado, t => t.videojuego.idJuego == idJuego)

  def tipoDeTorneo(torneo: Torneo): String = torneo match
    case _: TorneoEliminacionDirecta => "Eliminación directa"
    case _: TorneoDobleEliminacion   => "Doble eliminación"
    case _: TorneoSistemaDePuntos    => "Sistema de puntos"
    case _                           => "Otro"

  def resumir(torneo: Torneo): ResumenTorneo =
    ResumenTorneo(
      torneo.idTorneo, torneo.nombreTorneo, tipoDeTorneo(torneo),
      torneo.videojuego.titulo, torneo.modalidad.descripcion(),
      torneo.estado.descripcion(), torneo.equipos.length)

  def resumenes(estado: EstadoSistema): List[ResumenTorneo] =
    estado.torneos.map(torneo => resumir(torneo))
}

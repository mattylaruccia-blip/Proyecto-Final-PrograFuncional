// =====================================================================
//  Interfaz gráfica (Java Swing) del gestor de torneos de videojuegos.
//
//  - No agrega dependencias: Swing viene incluido en el JDK.
//  - Toda la lógica de negocio se delega en `Sistema`, `Validaciones` y las
//    clases del dominio. Esta interfaz solo lee campos, llama a esas funciones
//    y muestra el `Resultado` (Exito -> actualiza la pantalla, Fallo -> avisa).
//  - El estado es un único `EstadoSistema` inmutable que se reemplaza por el
//    nuevo solo cuando una operación devuelve `Exito`.
//
//  Para ejecutarla:   InterfazGrafica   (objeto con el método main)
// =====================================================================
import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.{BorderLayout, CardLayout, Color, Component, Dimension, FlowLayout, Font, GridBagConstraints, GridBagLayout, Insets}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

import Ui.Opcion

// ---------------------------------------------------------------------
//  Validación de lo que escribe la persona (devuelve Resultado, como el resto del proyecto)
// ---------------------------------------------------------------------
object Entradas {

  def requerido(texto: String, campo: String): Resultado[String] =
    if texto.trim.isEmpty then Fallo[String](s"El campo '$campo' es obligatorio")
    else Exito[String](texto.trim)

  def entero(texto: String, campo: String): Resultado[Int] =
    Try(texto.trim.toInt).toOption match
      case Some(n) => Exito[Int](n)
      case None    => Fallo[Int](s"'$campo' debe ser un número entero")

  def noNegativo(texto: String, campo: String): Resultado[Int] =
    entero(texto, campo).encadenar(n =>
      if n >= 0 then Exito[Int](n) else Fallo[Int](s"'$campo' no puede ser negativo"))

  def correo(texto: String): Resultado[String] =
    requerido(texto, "Correo").encadenar(t =>
      if t.contains("@") then Exito[String](t) else Fallo[String]("El correo debe contener '@'"))

  def presente[A](opcion: Option[A], mensaje: String): Resultado[A] = opcion match
    case Some(valor) => Exito[A](valor)
    case None        => Fallo[A](mensaje)

  def combinar[A, B](a: Resultado[A], b: => Resultado[B]): Resultado[(A, B)] =
    a.encadenar(x => b.mapear(y => (x, y)))

  // Convierte List[Resultado[A]] en Resultado[List[A]]: falla con el primer error.
  def secuencia[A](resultados: List[Resultado[A]]): Resultado[List[A]] =
    resultados.foldRight(Exito[List[A]](Nil): Resultado[List[A]])((r, acc) =>
      r.encadenar(x => acc.mapear(xs => x :: xs)))
}

// ---------------------------------------------------------------------
//  Utilidades de Swing para no repetir código
// ---------------------------------------------------------------------
object Ui {

  // Elemento de un combo: muestra `texto`, guarda `valor`. `clave` sirve para
  // conservar la selección cuando el combo se vuelve a llenar.
  case class Opcion[A](valor: A, texto: String, clave: Any = null) {
    override def toString: String = texto
    def identidad: Any = if clave == null then texto else clave
  }

  def nombrar[T <: Component](componente: T, nombre: String): T = {
    componente.setName(nombre)
    componente
  }

  def boton(texto: String)(accion: => Unit): JButton = {
    val b = new JButton(texto)
    b.addActionListener(_ => accion)
    b
  }

  def campo(nombre: String, columnas: Int = 16): JTextField =
    nombrar(new JTextField(columnas), nombre)

  def margen(p: JComponent, px: Int = 10): Unit =
    p.setBorder(BorderFactory.createEmptyBorder(px, px, px, px))

  def conTitulo(p: JComponent, titulo: String): Unit =
    p.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder(titulo),
      BorderFactory.createEmptyBorder(4, 6, 6, 6)))

  def desplazable(componente: Component, ancho: Int, alto: Int): JScrollPane = {
    val sp = new JScrollPane(componente)
    sp.setPreferredSize(new Dimension(ancho, alto))
    sp
  }

  def areaTexto(nombre: String, filas: Int): JTextArea = {
    val a = nombrar(new JTextArea(filas, 30), nombre)
    a.setEditable(false)
    a.setLineWrap(true)
    a.setWrapStyleWord(true)
    a.setMargin(new Insets(6, 8, 6, 8))
    a
  }

  def tablaSoloLectura(nombre: String, columnas: String*): (JTable, DefaultTableModel) = {
    val modelo = new DefaultTableModel(columnas.toArray[AnyRef], 0) {
      override def isCellEditable(fila: Int, columna: Int): Boolean = false
    }
    val t = nombrar(new JTable(modelo), nombre)
    t.setRowHeight(26)
    t.setFillsViewportHeight(true)
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    t.getTableHeader.setReorderingAllowed(false)
    (t, modelo)
  }

  def llenar(modelo: DefaultTableModel, filas: List[List[Any]]): Unit = {
    modelo.setRowCount(0)
    filas.foreach(fila => modelo.addRow(fila.map(_.asInstanceOf[AnyRef]).toArray))
  }

  def alSeleccionar(tabla: JTable)(accion: => Unit): Unit =
    tabla.getSelectionModel.addListSelectionListener(e => if !e.getValueIsAdjusting then accion)

  // Índice de la fila seleccionada, si hay una.
  def filaSeleccionada(tabla: JTable): Option[Int] =
    Some(tabla.getSelectedRow).filter(_ >= 0)

  def combo[A](nombre: String): JComboBox[Opcion[A]] =
    nombrar(new JComboBox[Opcion[A]](), nombre)

  // Vuelve a llenar el combo conservando (si existe) la opción que estaba elegida.
  def rellenar[A](cb: JComboBox[Opcion[A]], opciones: List[Opcion[A]]): Unit = {
    val anterior = Option(cb.getSelectedItem).map(_.asInstanceOf[Opcion[A]].identidad)
    cb.removeAllItems()
    opciones.foreach(o => cb.addItem(o))
    anterior.flatMap(id => opciones.find(_.identidad == id)).foreach(o => cb.setSelectedItem(o))
  }

  def seleccion[A](cb: JComboBox[Opcion[A]]): Option[A] =
    Option(cb.getSelectedItem).map(_.asInstanceOf[Opcion[A]].valor)

  // Formulario: pares etiqueta + componente, `columnas` pares por renglón.
  def formulario(filas: List[(String, Component)], columnas: Int = 1): JPanel = {
    val panel = new JPanel(new GridBagLayout)
    filas.zipWithIndex.foreach { case ((texto, componente), i) =>
      val renglon = i / columnas
      val bloque = i % columnas
      val izq = new GridBagConstraints()
      izq.gridx = bloque * 2; izq.gridy = renglon; izq.anchor = GridBagConstraints.WEST
      izq.insets = new Insets(4, if bloque == 0 then 4 else 16, 4, 8)
      panel.add(new JLabel(texto), izq)
      val der = new GridBagConstraints()
      der.gridx = bloque * 2 + 1; der.gridy = renglon; der.fill = GridBagConstraints.HORIZONTAL
      der.weightx = 1.0; der.insets = new Insets(4, 0, 4, 4)
      panel.add(componente, der)
    }
    panel
  }

  def fila(componentes: Component*): JPanel = {
    val p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4))
    componentes.foreach(c => p.add(c))
    p
  }
}

// ---------------------------------------------------------------------
//  Controlador: guarda el EstadoSistema y avisa a las pantallas cuando cambia
// ---------------------------------------------------------------------
class Controlador {
  private var estadoActual: EstadoSistema = Sistema.estadoInicial
  private var observadores: List[() => Unit] = Nil

  var ventana: Component = null                       // para centrar los diálogos
  var alMensaje: (String, Boolean) => Unit = (_, _) => ()

  def estado: EstadoSistema = estadoActual

  def suscribir(observador: () => Unit): Unit = observadores = observadores :+ observador
  def notificar(): Unit = observadores.foreach(o => o())

  def avisar(texto: String): Unit = alMensaje(texto, false)

  def error(texto: String): Unit = {
    alMensaje(texto, true)
    JOptionPane.showMessageDialog(ventana, texto, "No se pudo completar la acción", JOptionPane.WARNING_MESSAGE)
  }

  def confirmar(pregunta: String): Boolean =
    JOptionPane.showConfirmDialog(ventana, pregunta, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION

  // Aplica una operación de `Sistema`. Si sale bien reemplaza el estado y refresca todo;
  // si falla muestra el motivo y el estado queda igual. Devuelve si tuvo éxito.
  def ejecutar(operacion: EstadoSistema => Resultado[EstadoSistema], mensajeExito: String): Boolean =
    operacion(estadoActual) match
      case Exito(nuevo) =>
        estadoActual = nuevo
        avisar(mensajeExito)
        notificar()
        true
      case Fallo(motivo) =>
        error(motivo)
        false
}

// ---------------------------------------------------------------------
//  Pestaña 1: Videojuegos
// ---------------------------------------------------------------------
class PanelVideojuegos(c: Controlador) extends JPanel(new BorderLayout(8, 8)) {
  private val txtTitulo = Ui.campo("txtTituloJuego")
  private val txtGenero = Ui.campo("txtGeneroJuego")
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaVideojuegos", "ID", "Título", "Género")

  private def registrar(): Unit =
    Entradas.combinar(
      Entradas.requerido(txtTitulo.getText, "Título"),
      Entradas.requerido(txtGenero.getText, "Género")) match
      case Fallo(motivo) => c.error(motivo)
      case Exito((titulo, genero)) =>
        val id = Sistema.siguienteId(c.estado.videojuegos, (v: Videojuego) => v.idJuego)
        val videojuego = Videojuego(id, titulo, genero)
        if c.ejecutar(e => Sistema.registrarVideojuego(e, videojuego), s"Videojuego '$titulo' registrado" ) then {
          txtTitulo.setText("")
          txtGenero.setText("")
          txtTitulo.requestFocusInWindow()
        }

  private def refrescar(): Unit =
    Ui.llenar(modelo, c.estado.videojuegos.map(v => List(v.idJuego, v.titulo, v.genero)))

  Ui.margen(this)
  private val form = Ui.formulario(List(("Título:", txtTitulo), ("Género:", txtGenero)))
  private val panelForm = new JPanel(new BorderLayout())
  panelForm.add(form, BorderLayout.CENTER)
  panelForm.add(Ui.fila(Ui.boton("Registrar videojuego")(registrar())), BorderLayout.SOUTH)
  Ui.conTitulo(panelForm, "Nuevo videojuego")
  add(panelForm, BorderLayout.NORTH)
  add(new JScrollPane(tabla), BorderLayout.CENTER)
  txtGenero.addActionListener(_ => registrar())
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
//  Pestaña 2: Equipos (con sus participantes)
// ---------------------------------------------------------------------
class PanelEquipos(c: Controlador) extends JPanel(new BorderLayout(8, 8)) {
  private val txtEquipo = Ui.campo("txtNombreEquipo")
  private val txtNick = Ui.campo("txtNick")
  private val txtNombre = Ui.campo("txtNombreParticipante")
  private val txtCorreo = Ui.campo("txtCorreo")
  private val modeloBorrador = new DefaultListModel[String]()
  private val listaBorrador = Ui.nombrar(new JList[String](modeloBorrador), "listaBorrador")
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaEquipos", "ID", "Equipo", "N.º integrantes", "Nicknames")

  // Integrantes que se están armando para el equipo nuevo (todavía no registrado).
  private var borrador: List[Participante] = Nil

  private def idParticipantes: Int =
    Sistema.siguienteId(c.estado.equipos.flatMap(_.integrantes) ++ borrador, (p: Participante) => p.idParticipante)

  private def mostrarBorrador(): Unit = {
    modeloBorrador.clear()
    borrador.foreach(p => modeloBorrador.addElement(s"${p.nickname} — ${p.nombre} (${p.correo})"))
  }

  private def agregarIntegrante(): Unit =
    Entradas.combinar(
      Entradas.requerido(txtNick.getText, "Nickname"),
      Entradas.combinar(Entradas.requerido(txtNombre.getText, "Nombre"), Entradas.correo(txtCorreo.getText))) match
      case Fallo(motivo) => c.error(motivo)
      case Exito((nick, (nombre, correo))) =>
        val id = idParticipantes
        borrador = borrador :+ Participante(id, nick, id, nombre, correo)
        mostrarBorrador()
        List(txtNick, txtNombre, txtCorreo).foreach(t => t.setText(""))
        txtNick.requestFocusInWindow()

  private def quitarUltimo(): Unit = {
    borrador = borrador.dropRight(1)
    mostrarBorrador()
  }

  private def registrarEquipo(): Unit =
    Entradas.requerido(txtEquipo.getText, "Nombre del equipo").encadenar(nombre =>
      if borrador.isEmpty then Fallo[String]("Agrega al menos un integrante al equipo")
      else Exito[String](nombre)) match
      case Fallo(motivo) => c.error(motivo)
      case Exito(nombre) =>
        val id = Sistema.siguienteId(c.estado.equipos, (e: Equipo) => e.idEquipo)
        val equipo = Equipo(id, nombre, borrador)
        if c.ejecutar(e => Sistema.registrarEquipo(e, equipo), s"Equipo '$nombre' registrado") then {
          borrador = Nil
          mostrarBorrador()
          txtEquipo.setText("")
        }

  private def refrescar(): Unit =
    Ui.llenar(modelo, c.estado.equipos.map(e =>
      List(e.idEquipo, e.nombreEquipo, e.integrantes.length, e.integrantes.map(_.nickname).mkString(", "))))

  Ui.margen(this)

  private val panelIntegrante = Ui.formulario(List(
    ("Nickname:", txtNick), ("Nombre:", txtNombre), ("Correo:", txtCorreo)))
  private val botonesIntegrante = Ui.fila(
    Ui.boton("Agregar integrante")(agregarIntegrante()),
    Ui.boton("Quitar último")(quitarUltimo()))
  private val izquierda = new JPanel(new BorderLayout())
  izquierda.add(panelIntegrante, BorderLayout.CENTER)
  izquierda.add(botonesIntegrante, BorderLayout.SOUTH)
  Ui.conTitulo(izquierda, "1. Agregar integrantes")

  private val derecha = new JPanel(new BorderLayout(4, 4))
  derecha.add(Ui.formulario(List(("Nombre del equipo:", txtEquipo))), BorderLayout.NORTH)
  private val panelLista = Ui.desplazable(listaBorrador, 260, 110)
  panelLista.setBorder(BorderFactory.createTitledBorder("Integrantes del equipo nuevo"))
  derecha.add(panelLista, BorderLayout.CENTER)
  derecha.add(Ui.fila(Ui.boton("Registrar equipo")(registrarEquipo())), BorderLayout.SOUTH)
  Ui.conTitulo(derecha, "2. Registrar el equipo")

  private val arriba = new JPanel(new java.awt.GridLayout(1, 2, 8, 0))
  arriba.add(izquierda)
  arriba.add(derecha)

  add(arriba, BorderLayout.NORTH)
  add(new JScrollPane(tabla), BorderLayout.CENTER)
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
//  Pestaña 3: Torneos (crear, listar, filtrar, iniciar y finalizar)
// ---------------------------------------------------------------------
class PanelTorneos(c: Controlador) extends JPanel(new BorderLayout(8, 8)) {

  // Datos necesarios para construir cualquiera de los tres tipos de torneo.
  private case class DatosTorneo(id: Int, nombre: String, juego: Videojuego,
                                 modalidad: ModalidadTorneo, peso: Int, baja: Int)

  private val IndicePuntos = 0
  private val tipos: List[Opcion[DatosTorneo => Torneo]] = List(
    Opcion[DatosTorneo => Torneo]((d: DatosTorneo) =>
      TorneoSistemaDePuntos(d.peso, d.baja, d.id, d.nombre, d.juego, d.modalidad, Nil, TorneoPendiente),
      "Sistema de puntos"),
    Opcion[DatosTorneo => Torneo]((d: DatosTorneo) =>
      TorneoEliminacionDirecta(d.id, d.nombre, d.juego, d.modalidad, Nil, TorneoPendiente),
      "Eliminación directa"),
    Opcion[DatosTorneo => Torneo]((d: DatosTorneo) => {
      val ganadores = TorneoEliminacionDirecta(d.id, s"${d.nombre} (ganadores)", d.juego, d.modalidad, Nil, TorneoPendiente)
      val perdedores = TorneoEliminacionDirecta(d.id, s"${d.nombre} (perdedores)", d.juego, d.modalidad, Nil, TorneoPendiente)
      TorneoDobleEliminacion(ganadores, perdedores, d.id, d.nombre, d.juego, d.modalidad, Nil, TorneoPendiente)
    }, "Doble eliminación"))

  private val modalidades: List[Opcion[ModalidadTorneo]] =
    List[ModalidadTorneo](Individual, PorEquipos).map(m => Opcion(m, m.descripcion()))

  private val txtNombre = Ui.campo("txtNombreTorneo", 20)
  private val txtPeso = Ui.campo("txtPeso", 10)
  private val txtBaja = Ui.campo("txtBaja", 10)
  private val cbJuego = Ui.combo[Videojuego]("cbJuegoTorneo")
  private val cbTipo = Ui.combo[DatosTorneo => Torneo]("cbTipoTorneo")
  private val cbModalidad = Ui.combo[ModalidadTorneo]("cbModalidadTorneo")
  private val lblRequisito = new JLabel(" ")
  private val cbFiltro = Ui.combo[EstadoSistema => List[Torneo]]("cbFiltroTorneos")
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaTorneos",
    "ID", "Torneo", "Tipo", "Videojuego", "Modalidad", "Estado", "Equipos")
  private val txtDetalle = Ui.areaTexto("txtDetalleTorneo", 5)

  private var visibles: List[Torneo] = Nil

  txtPeso.setText("10")
  txtBaja.setText("2")
  tipos.foreach(t => cbTipo.addItem(t))
  modalidades.foreach(m => cbModalidad.addItem(m))

  private def esPuntos: Boolean = cbTipo.getSelectedIndex == IndicePuntos

  private def actualizarFormulario(): Unit = {
    txtPeso.setEnabled(esPuntos)
    txtBaja.setEnabled(esPuntos)
    lblRequisito.setText(Ui.seleccion(cbModalidad)
      .map(m => s"Modalidad ${m.descripcion()}: ${m.requisito()}").getOrElse(" "))
  }

  private def crearTorneo(): Unit = {
    val id = Sistema.siguienteId(c.estado.torneos, (t: Torneo) => t.idTorneo)
    val pesos: Resultado[(Int, Int)] =
      if esPuntos then Entradas.combinar(
        Entradas.noNegativo(txtPeso.getText, "Peso de clasificación"),
        Entradas.noNegativo(txtBaja.getText, "Puntaje por baja"))
      else Exito[(Int, Int)]((0, 0))

    val solicitud: Resultado[Torneo] =
      Entradas.requerido(txtNombre.getText, "Nombre del torneo").encadenar(nombre =>
        pesos.encadenar((peso, baja) =>
          Entradas.presente(Ui.seleccion(cbJuego), "Primero registra un videojuego (pestaña Videojuegos)").encadenar(juego =>
            Entradas.presente(Ui.seleccion(cbModalidad), "Elige una modalidad").encadenar(modalidad =>
              Entradas.presente(Ui.seleccion(cbTipo), "Elige un tipo de torneo").mapear(construir =>
                construir(DatosTorneo(id, nombre, juego, modalidad, peso, baja)))))))

    solicitud match
      case Fallo(motivo) => c.error(motivo)
      case Exito(torneo) =>
        if c.ejecutar(e => Sistema.agregarTorneo(e, torneo), s"Torneo '${torneo.nombreTorneo}' creado (pendiente)") then
          txtNombre.setText("")
  }

  private def seleccionado: Option[Torneo] =
    Ui.filaSeleccionada(tabla).flatMap(i => visibles.lift(i))

  private def cambiarEstado(nuevo: EstadoTorneo, mensaje: String): Unit =
    seleccionado match
      case None => c.error("Selecciona un torneo de la tabla")
      case Some(t) => c.ejecutar(e => Sistema.cambiarEstadoTorneo(e, t.idTorneo, nuevo), s"${t.nombreTorneo}: $mensaje")

  private def finalizar(): Unit =
    seleccionado match
      case None => c.error("Selecciona un torneo de la tabla")
      case Some(t) =>
        if c.confirmar(s"¿Finalizar el torneo '${t.nombreTorneo}'? Esta acción no se puede deshacer.") then
          cambiarEstado(TorneoFinalizado, "torneo finalizado")

  private def detalle(t: Torneo): String = {
    val extra = t match
      case p: TorneoSistemaDePuntos =>
        s"\nPeso de clasificación: ${p.pesoClasificacion}\nPuntaje por baja: ${p.puntajePorBaja}"
      case _ => ""
    val equipos =
      if t.equipos.isEmpty then "  (todavía no hay equipos inscritos)"
      else t.equipos.map(e => s"  - ${e.nombreEquipo} (${e.integrantes.length} integrante(s))").mkString("\n")
    s"${t.descripción()}$extra\nRequisito: ${t.modalidad.requisito()}\n\nEquipos inscritos (${t.equipos.length}):\n$equipos"
  }

  private def mostrarDetalle(): Unit = {
    txtDetalle.setText(seleccionado.map(t => detalle(t)).getOrElse("Selecciona un torneo de la tabla para ver su detalle."))
    txtDetalle.setCaretPosition(0)
  }

  private def refrescar(): Unit = {
    val estado = c.estado
    Ui.rellenar(cbJuego, estado.videojuegos.map(v => Opcion(v, s"${v.titulo} (${v.genero})", v.idJuego)))

    val filtros: List[Opcion[EstadoSistema => List[Torneo]]] =
      List(
        Opcion[EstadoSistema => List[Torneo]]((e: EstadoSistema) => e.torneos, "Todos los torneos", "todos"),
        Opcion[EstadoSistema => List[Torneo]]((e: EstadoSistema) => Sistema.torneosActivos(e), "Solo torneos activos", "activos")) ++
        estado.videojuegos.map(v =>
          Opcion[EstadoSistema => List[Torneo]]((e: EstadoSistema) => Sistema.torneosPorVideojuego(e, v.idJuego), s"Videojuego: ${v.titulo}", s"juego-${v.idJuego}"))
    Ui.rellenar(cbFiltro, filtros)

    val idPrevio = seleccionado.map(_.idTorneo)
    visibles = Ui.seleccion(cbFiltro).map(f => f(estado)).getOrElse(estado.torneos)
    Ui.llenar(modelo, visibles.map(t => {
      val r = Sistema.resumir(t)
      List(r.id, r.nombre, r.tipo, r.videojuego, r.modalidad, r.estado, r.cantidadEquipos)
    }))
    idPrevio.map(id => visibles.indexWhere(_.idTorneo == id)).filter(_ >= 0)
      .foreach(i => tabla.setRowSelectionInterval(i, i))
    mostrarDetalle()
  }

  Ui.margen(this)

  private val formulario = Ui.formulario(List(
    ("Nombre:", txtNombre), ("Videojuego:", cbJuego), ("Tipo:", cbTipo), ("Modalidad:", cbModalidad),
    ("Peso clasificación:", txtPeso), ("Puntaje por baja:", txtBaja)), 2)
  private val panelNuevo = new JPanel(new BorderLayout())
  panelNuevo.add(formulario, BorderLayout.CENTER)
  private val pie = new JPanel(new BorderLayout())
  pie.add(lblRequisito, BorderLayout.NORTH)
  pie.add(Ui.fila(Ui.boton("Crear torneo")(crearTorneo())), BorderLayout.SOUTH)
  panelNuevo.add(pie, BorderLayout.SOUTH)
  Ui.conTitulo(panelNuevo, "Nuevo torneo")

  private val barra = Ui.fila(
    new JLabel("Mostrar:"), cbFiltro,
    Ui.boton("Iniciar torneo")(cambiarEstado(TorneoEnCurso, "torneo iniciado")),
    Ui.boton("Finalizar torneo")(finalizar()))
  private val centro = new JPanel(new BorderLayout(4, 4))
  centro.add(barra, BorderLayout.NORTH)
  centro.add(new JScrollPane(tabla), BorderLayout.CENTER)
  centro.add(Ui.desplazable(txtDetalle, 400, 110), BorderLayout.SOUTH)

  add(panelNuevo, BorderLayout.NORTH)
  add(centro, BorderLayout.CENTER)

  cbTipo.addActionListener(_ => actualizarFormulario())
  cbModalidad.addActionListener(_ => actualizarFormulario())
  cbFiltro.addActionListener(_ => if cbFiltro.getItemCount > 0 then refrescar())
  Ui.alSeleccionar(tabla)(mostrarDetalle())
  actualizarFormulario()
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
//  Pestaña 4: Inscripciones
// ---------------------------------------------------------------------
class PanelInscripciones(c: Controlador) extends JPanel(new BorderLayout(8, 8)) {
  private val cbTorneo = Ui.combo[Torneo]("cbTorneoInscripcion")
  private val cbEquipo = Ui.combo[Equipo]("cbEquipoInscripcion")
  private val lblInfo = new JLabel(" ")
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaInscritos", "ID", "Equipo", "Integrantes")

  private def inscribir(): Unit =
    Entradas.combinar(
      Entradas.presente(Ui.seleccion(cbTorneo), "No hay torneos creados (pestaña Torneos)"),
      Entradas.presente(Ui.seleccion(cbEquipo), "No hay equipos registrados (pestaña Equipos)")) match
      case Fallo(motivo) => c.error(motivo)
      case Exito((torneo, equipo)) =>
        c.ejecutar(e => Sistema.inscribirEquipo(e, torneo.idTorneo, equipo.idEquipo),
          s"Equipo '${equipo.nombreEquipo}' inscrito en '${torneo.nombreTorneo}'")

  private def refrescarDetalle(): Unit =
    Ui.seleccion(cbTorneo) match
      case None =>
        lblInfo.setText(" ")
        Ui.llenar(modelo, Nil)
      case Some(t) =>
        lblInfo.setText(s"Estado: ${t.estado.descripcion()}  |  Modalidad ${t.modalidad.descripcion()}: ${t.modalidad.requisito()}")
        Ui.llenar(modelo, t.equipos.map(e => List(e.idEquipo, e.nombreEquipo, e.integrantes.length)))

  private def refrescar(): Unit = {
    Ui.rellenar(cbTorneo, c.estado.torneos.map(t =>
      Opcion(t, s"#${t.idTorneo} ${t.nombreTorneo} [${t.estado.descripcion()}]", t.idTorneo)))
    Ui.rellenar(cbEquipo, c.estado.equipos.map(e =>
      Opcion(e, s"#${e.idEquipo} ${e.nombreEquipo} (${e.integrantes.length} integrante(s))", e.idEquipo)))
    refrescarDetalle()
  }

  Ui.margen(this)
  private val panelForm = new JPanel(new BorderLayout())
  panelForm.add(Ui.formulario(List(("Torneo:", cbTorneo), ("Equipo:", cbEquipo))), BorderLayout.CENTER)
  private val pie = new JPanel(new BorderLayout())
  pie.add(lblInfo, BorderLayout.NORTH)
  pie.add(Ui.fila(Ui.boton("Inscribir equipo")(inscribir())), BorderLayout.SOUTH)
  panelForm.add(pie, BorderLayout.SOUTH)
  Ui.conTitulo(panelForm, "Inscribir un equipo en un torneo (solo torneos pendientes)")

  private val listado = new JPanel(new BorderLayout())
  listado.add(new JScrollPane(tabla), BorderLayout.CENTER)
  Ui.conTitulo(listado, "Equipos inscritos en el torneo seleccionado")

  add(panelForm, BorderLayout.NORTH)
  add(listado, BorderLayout.CENTER)
  cbTorneo.addActionListener(_ => refrescarDetalle())
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
//  Pestaña 5: Puntuación (torneos por sistema de puntos -> TablaPosiciones)
// ---------------------------------------------------------------------
class PanelPuntuacion(c: Controlador) extends JPanel(new BorderLayout(8, 8)) {
  private val cbTorneo = Ui.combo[TorneoSistemaDePuntos]("cbTorneoPuntos")
  private val lblFormula = new JLabel(" ")
  private val txtFecha = Ui.campo("txtFecha", 10)
  private val txtRondas = Ui.areaTexto("txtRondas", 5)
  private val txtPodio = Ui.areaTexto("txtPodio", 4)
  private val (tablaResultados, modeloResultados) = Ui.tablaSoloLectura("tablaResultados", "Puesto", "Equipo", "Puntos")

  // Tabla editable: la persona escribe la posición y las bajas de cada equipo en la ronda.
  private val modeloRonda = new DefaultTableModel(Array[AnyRef]("Equipo", "Posición", "Bajas"), 0) {
    override def isCellEditable(fila: Int, columna: Int): Boolean = columna > 0
    override def getColumnClass(columna: Int): Class[?] =
      if columna == 0 then classOf[String] else classOf[Integer]
  }
  private val tablaRonda = Ui.nombrar(new JTable(modeloRonda), "tablaRonda")
  tablaRonda.setRowHeight(26)
  tablaRonda.setFillsViewportHeight(true)
  tablaRonda.getTableHeader.setReorderingAllowed(false)

  // Rondas ya registradas por torneo (en la interfaz; el cálculo lo hace TorneoSistemaDePuntos).
  private var rondas: Map[Int, List[List[DesempenoEquipo]]] = Map.empty

  private def rondasDe(idTorneo: Int): List[List[DesempenoEquipo]] = rondas.getOrElse(idTorneo, Nil)

  private def valorEntero(fila: Int, columna: Int, campo: String): Resultado[Int] =
    modeloRonda.getValueAt(fila, columna) match
      case n: Integer => Exito[Int](n.intValue)
      case otro       => Entradas.entero(String.valueOf(otro), campo)

  private def leerRonda(t: TorneoSistemaDePuntos): Resultado[List[DesempenoEquipo]] = {
    if tablaRonda.isEditing then tablaRonda.getCellEditor.stopCellEditing()
    Entradas.secuencia(t.equipos.zipWithIndex.map((equipo, i) =>
      Entradas.combinar(valorEntero(i, 1, "Posición"), valorEntero(i, 2, "Bajas"))
        .mapear((posicion, bajas) => DesempenoEquipo(equipo, posicion, bajas))))
  }

  private def cargarRonda(): Unit = {
    modeloRonda.setRowCount(0)
    Ui.seleccion(cbTorneo).map(_.equipos).getOrElse(Nil).zipWithIndex.foreach((e, i) =>
      modeloRonda.addRow(Array[AnyRef](e.nombreEquipo, Int.box(i + 1), Int.box(0))))
  }

  private def textoRondas(): String =
    Ui.seleccion(cbTorneo).map(t => rondasDe(t.idTorneo)) match
      case None | Some(Nil) => "Aún no hay rondas registradas."
      case Some(lista) =>
        lista.zipWithIndex.map((ronda, i) =>
          s"Ronda ${i + 1}: " + ronda.map(d => s"${d.equipo.nombreEquipo} (puesto ${d.posicion}, ${d.bajas} bajas)").mkString("; ")
        ).mkString("\n")

  private def limpiarResultados(): Unit = {
    Ui.llenar(modeloResultados, Nil)
    txtPodio.setText("")
  }

  private def alCambiarTorneo(): Unit = {
    lblFormula.setText(Ui.seleccion(cbTorneo).map(t =>
      s"Puntos por ronda = (equipos − puesto + 1) × ${t.pesoClasificacion} + bajas × ${t.puntajePorBaja}   |   ${t.equipos.length} equipo(s) inscrito(s)"
    ).getOrElse("No hay torneos por sistema de puntos. Crea uno en la pestaña Torneos."))
    txtRondas.setText(textoRondas())
    cargarRonda()
    limpiarResultados()
  }

  private def agregarRonda(): Unit =
    Ui.seleccion(cbTorneo) match
      case None => c.error("No hay un torneo por sistema de puntos seleccionado")
      case Some(t) =>
        leerRonda(t).encadenar(ronda => t.validarRonda(ronda)) match
          case Fallo(motivo) => c.error(motivo)
          case Exito(ronda) =>
            rondas = rondas.updated(t.idTorneo, rondasDe(t.idTorneo) :+ ronda)
            c.avisar(s"Ronda ${rondasDe(t.idTorneo).length} registrada")
            alCambiarTorneo()

  private def quitarRonda(): Unit =
    Ui.seleccion(cbTorneo).foreach(t => {
      rondas = rondas.updated(t.idTorneo, rondasDe(t.idTorneo).dropRight(1))
      alCambiarTorneo()
    })

  private def calcular(): Unit =
    Entradas.combinar(
      Entradas.presente(Ui.seleccion(cbTorneo), "No hay un torneo por sistema de puntos seleccionado"),
      Entradas.requerido(txtFecha.getText, "Fecha")).encadenar((t, fecha) =>
        if rondasDe(t.idTorneo).isEmpty then Fallo[(Int, String, TablaPosiciones)]("Registra al menos una ronda antes de calcular la tabla")
        else Sistema.calcularTablaPuntos(c.estado, t.idTorneo, rondasDe(t.idTorneo), fecha).mapear(tabla => (t.idTorneo, fecha, tabla))
      ) match
      case Fallo(motivo) => c.error(motivo)
      case Exito((_, fecha, tabla)) =>
        val ordenados = tabla.equipos.sortBy(r => -r.puntosTotales)
        Ui.llenar(modeloResultados, ordenados.zipWithIndex.map((r, i) => List(i + 1, r.equipo.nombreEquipo, r.puntosTotales)))
        val podio = ordenados.take(3).zipWithIndex.map((r, i) => s"${i + 1}.º  ${r.descripcionResultado()}")
        txtPodio.setText(s"Podio (${tabla.torneo.nombreTorneo}, $fecha)\n" + podio.mkString("\n"))
        c.avisar("Tabla de posiciones calculada")

  private def refrescar(): Unit = {
    val torneos = c.estado.torneos.collect { case t: TorneoSistemaDePuntos => t }
    Ui.rellenar(cbTorneo, torneos.map(t => Opcion(t, s"#${t.idTorneo} ${t.nombreTorneo} [${t.estado.descripcion()}]", t.idTorneo)))
    alCambiarTorneo()
  }

  Ui.margen(this)
  txtFecha.setText(LocalDate.now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))

  private val arriba = new JPanel(new BorderLayout())
  arriba.add(Ui.formulario(List(("Torneo:", cbTorneo))), BorderLayout.CENTER)
  arriba.add(lblFormula, BorderLayout.SOUTH)

  private val izquierda = new JPanel(new BorderLayout(4, 4))
  izquierda.add(new JLabel("Edita la posición y las bajas de cada equipo y pulsa «Agregar ronda»."), BorderLayout.NORTH)
  izquierda.add(new JScrollPane(tablaRonda), BorderLayout.CENTER)
  private val sur = new JPanel(new BorderLayout(4, 4))
  sur.add(Ui.fila(Ui.boton("Agregar ronda")(agregarRonda()), Ui.boton("Quitar última ronda")(quitarRonda())), BorderLayout.NORTH)
  sur.add(Ui.desplazable(txtRondas, 300, 100), BorderLayout.CENTER)
  izquierda.add(sur, BorderLayout.SOUTH)
  Ui.conTitulo(izquierda, "1. Rondas")

  private val derecha = new JPanel(new BorderLayout(4, 4))
  derecha.add(Ui.fila(new JLabel("Fecha:"), txtFecha, Ui.boton("Calcular tabla de posiciones")(calcular())), BorderLayout.NORTH)
  derecha.add(new JScrollPane(tablaResultados), BorderLayout.CENTER)
  derecha.add(Ui.desplazable(txtPodio, 300, 95), BorderLayout.SOUTH)
  Ui.conTitulo(derecha, "2. Tabla de posiciones")

  private val division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha)
  division.setResizeWeight(0.5)

  add(arriba, BorderLayout.NORTH)
  add(division, BorderLayout.CENTER)
  cbTorneo.addActionListener(_ => alCambiarTorneo())
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
//  Pestaña 6: Partidas (PartidaPuntos y PartidaEliminacion)
//  Las partidas viven en la interfaz: el EstadoSistema del proyecto aún no las guarda.
// ---------------------------------------------------------------------
class PanelPartidas(c: Controlador) extends JPanel(new BorderLayout(8, 8)) {
  private val cbTorneo = Ui.combo[Torneo]("cbTorneoPartida")
  private val cbE1 = Ui.combo[Equipo]("cbEquipo1")
  private val cbE2 = Ui.combo[Equipo]("cbEquipo2")
  private val txtP1 = Ui.campo("txtPuntaje1", 6)
  private val txtP2 = Ui.campo("txtPuntaje2", 6)
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaPartidas", "ID", "Equipo 1", "Equipo 2", "Resultado")
  private val tarjetas = new CardLayout
  private val panelAccion = new JPanel(tarjetas)

  private var partidas: Map[Int, List[Partida]] = Map.empty

  private def partidasDe(idTorneo: Int): List[Partida] = partidas.getOrElse(idTorneo, Nil)

  private def refrescarTabla(): Unit =
    Ui.llenar(modelo, Ui.seleccion(cbTorneo).map(t => partidasDe(t.idTorneo)).getOrElse(Nil)
      .map(p => List(p.idPartida, p.equipo1.nombreEquipo, p.equipo2.nombreEquipo, p.resumen())))

  // Crea la partida con la función recibida; si sale bien la agrega a la lista del torneo.
  private def agregarPartida(construir: (Int, Equipo, Equipo) => Resultado[Partida]): Unit =
    (Ui.seleccion(cbTorneo), Ui.seleccion(cbE1), Ui.seleccion(cbE2)) match
      case (Some(t), Some(e1), Some(e2)) =>
        if e1.idEquipo == e2.idEquipo then c.error("Elige dos equipos distintos")
        else
          val id = Sistema.siguienteId(partidasDe(t.idTorneo), (p: Partida) => p.idPartida)
          construir(id, e1, e2) match
            case Fallo(motivo) => c.error(motivo)
            case Exito(partida) =>
              partidas = partidas.updated(t.idTorneo, partidasDe(t.idTorneo) :+ partida)
              refrescarTabla()
              c.avisar(s"Partida ${partida.idPartida} registrada: ${partida.resumen()}")
      case _ => c.error("Elige un torneo con al menos 2 equipos inscritos")

  private def registrarPuntos(): Unit =
    agregarPartida((id, e1, e2) =>
      Entradas.combinar(
        Entradas.entero(txtP1.getText, "Puntaje del equipo 1"),
        Entradas.entero(txtP2.getText, "Puntaje del equipo 2")).encadenar((p1, p2) =>
          PartidaPuntos(0, 0, id, e1, e2, EnCurso).actualizarPuntaje(p1, p2).mapear[Partida](p => p)))

  private def registrarGanador(estadoFinal: EstadoPartida): Unit =
    agregarPartida((id, e1, e2) =>
      Exito[Partida](PartidaEliminacion(id, e1, e2, EnCurso).actualizarEstadoPartida(estadoFinal)))

  private def alCambiarTorneo(): Unit = {
    val torneo = Ui.seleccion(cbTorneo)
    val opciones = torneo.map(_.equipos).getOrElse(Nil)
      .map(e => Opcion(e, s"#${e.idEquipo} ${e.nombreEquipo}", e.idEquipo))
    Ui.rellenar(cbE1, opciones)
    Ui.rellenar(cbE2, opciones)
    if cbE2.getItemCount > 1 && cbE1.getSelectedIndex == cbE2.getSelectedIndex then
      cbE2.setSelectedIndex((cbE1.getSelectedIndex + 1) % cbE2.getItemCount)
    torneo match
      case Some(_: TorneoSistemaDePuntos) => tarjetas.show(panelAccion, "puntos")
      case _                              => tarjetas.show(panelAccion, "eliminacion")
    refrescarTabla()
  }

  private def refrescar(): Unit = {
    val torneos = Sistema.filtrarTorneos(c.estado, t => t.equipos.length >= 2)
    Ui.rellenar(cbTorneo, torneos.map(t => Opcion(t, s"#${t.idTorneo} ${t.nombreTorneo} [${Sistema.tipoDeTorneo(t)}]", t.idTorneo)))
    alCambiarTorneo()
  }

  Ui.margen(this)
  txtP1.setText("0")
  txtP2.setText("0")

  private val panelPuntos = Ui.fila(new JLabel("Puntaje equipo 1:"), txtP1, new JLabel("Puntaje equipo 2:"), txtP2,
    Ui.boton("Registrar resultado")(registrarPuntos()))
  private val panelElim = Ui.fila(new JLabel("¿Quién ganó?"),
    Ui.boton("Gana el equipo 1")(registrarGanador(FinalizadoGanoEquipo1)),
    Ui.boton("Gana el equipo 2")(registrarGanador(FinalizadoGanoEquipo2)))
  panelAccion.add(panelPuntos, "puntos")
  panelAccion.add(panelElim, "eliminacion")

  private val form = new JPanel(new BorderLayout())
  form.add(Ui.formulario(List(("Torneo:", cbTorneo), ("Equipo 1:", cbE1), ("Equipo 2:", cbE2))), BorderLayout.CENTER)
  form.add(panelAccion, BorderLayout.SOUTH)
  Ui.conTitulo(form, "Registrar una partida (el torneo debe tener al menos 2 equipos inscritos)")

  private val listado = new JPanel(new BorderLayout())
  listado.add(new JScrollPane(tabla), BorderLayout.CENTER)
  Ui.conTitulo(listado, "Partidas del torneo seleccionado")

  add(form, BorderLayout.NORTH)
  add(listado, BorderLayout.CENTER)
  cbTorneo.addActionListener(_ => alCambiarTorneo())
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
//  Ventana principal
// ---------------------------------------------------------------------
class VentanaPrincipal extends JFrame("Gestor de Torneos de Videojuegos") {
  private val control = new Controlador
  private val lblEstado = Ui.nombrar(new JLabel(" "), "lblEstado")

  private def mostrarMensaje(texto: String, esError: Boolean): Unit = {
    lblEstado.setForeground(if esError then new Color(0xB3261E) else new Color(0x1B7F3B))
    lblEstado.setText(texto)
  }

  // Los mismos datos que usa Main.scala, más un torneo de eliminación directa.
  private def cargarEjemplo(): Unit = {
    val juego = Videojuego(1, "Call Of Duty", "FPS")
    val equipos = List(
      Equipo(1, "Triple t", List(Participante(1, "Tokkf", 1, "Diego", "a@gmail.com"))),
      Equipo(2, "Enanos Verdes", List(Participante(2, "La Trucha", 2, "Matias", "b@gmail.com"))),
      Equipo(3, "Gatitos feudales", List(Participante(3, "Leon", 3, "Paolo", "c@gmail.com"))),
      Equipo(4, "Los Cuatro", List(Participante(4, "Marco", 4, "Marco", "d@gmail.com"))),
      Equipo(5, "Dúo", List(Participante(5, "Ana", 5, "Ana", "e@gmail.com"), Participante(6, "Beto", 6, "Beto", "f@gmail.com"))))
    val torneoPuntos = TorneoSistemaDePuntos(10, 2, 1, "Copa Warzone", juego, Individual, Nil, TorneoPendiente)
    val torneoDirecta = TorneoEliminacionDirecta(2, "Torneo Relámpago", juego, Individual, Nil, TorneoPendiente)
    val inscripciones = List(1, 2).flatMap(idTorneo => List(1, 2, 3, 4).map(idEquipo => (idTorneo, idEquipo)))

    val operacion: EstadoSistema => Resultado[EstadoSistema] = inicio => {
      val conEquipos = equipos.foldLeft(Exito[EstadoSistema](inicio): Resultado[EstadoSistema])((acc, equipo) =>
        acc.encadenar(e => Sistema.registrarEquipo(e, equipo)))
      val preparado = conEquipos
        .encadenar(e => Sistema.registrarVideojuego(e, juego))
        .encadenar(e => Sistema.agregarTorneo(e, torneoPuntos))
        .encadenar(e => Sistema.agregarTorneo(e, torneoDirecta))
      inscripciones.foldLeft(preparado)((acc, par) =>
        acc.encadenar(e => Sistema.inscribirEquipo(e, par._1, par._2)))
    }
    control.ejecutar(operacion, "Datos de ejemplo cargados: 1 videojuego, 5 equipos y 2 torneos con 4 equipos inscritos")
  }

  private val pestanas = new JTabbedPane()
  pestanas.addTab("1. Videojuegos", new PanelVideojuegos(control))
  pestanas.addTab("2. Equipos", new PanelEquipos(control))
  pestanas.addTab("3. Torneos", new PanelTorneos(control))
  pestanas.addTab("4. Inscripciones", new PanelInscripciones(control))
  pestanas.addTab("5. Puntuación", new PanelPuntuacion(control))
  pestanas.addTab("6. Partidas", new PanelPartidas(control))

  private val titulo = new JLabel("Gestor de Torneos de Videojuegos")
  titulo.setFont(titulo.getFont.deriveFont(Font.BOLD, 20f))
  private val guia = new JLabel("Flujo: videojuego → equipos → torneo → inscripciones → iniciar torneo → partidas / puntuación")
  guia.setForeground(Color.GRAY)

  private val textos = new JPanel(new BorderLayout())
  textos.add(titulo, BorderLayout.NORTH)
  textos.add(guia, BorderLayout.SOUTH)
  private val encabezado = new JPanel(new BorderLayout())
  encabezado.add(textos, BorderLayout.WEST)
  encabezado.add(Ui.nombrar(Ui.boton("Cargar datos de ejemplo")(cargarEjemplo()), "btnEjemplo"), BorderLayout.EAST)
  Ui.margen(encabezado)

  private val barraEstado = new JPanel(new BorderLayout())
  barraEstado.add(lblEstado, BorderLayout.CENTER)
  barraEstado.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12))

  control.ventana = this
  control.alMensaje = mostrarMensaje
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  getContentPane.add(encabezado, BorderLayout.NORTH)
  getContentPane.add(pestanas, BorderLayout.CENTER)
  getContentPane.add(barraEstado, BorderLayout.SOUTH)
  setSize(1020, 700)
  setMinimumSize(new Dimension(860, 600))
  setLocationRelativeTo(null)
  control.notificar()
}

object InterfazGrafica {

  private def configurarEstilo(): Unit = {
    val esLinux = sys.props.getOrElse("os.name", "").toLowerCase.contains("linux")
    Try {
      UIManager.getInstalledLookAndFeels.find(_.getName == "Nimbus").filter(_ => esLinux) match
        case Some(info) => UIManager.setLookAndFeel(info.getClassName)
        case None       => UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
    }
    ()
  }

  def main(args: Array[String]): Unit =
    SwingUtilities.invokeLater(() => {
      configurarEstilo()
      new VentanaPrincipal().setVisible(true)
    })
}

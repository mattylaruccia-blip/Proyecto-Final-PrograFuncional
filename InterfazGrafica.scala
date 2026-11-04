// =====================================================================
// VEXORA — Interfaz gráfica (Java Swing) del gestor de torneos de videojuegos.
//
// - No agrega dependencias: Swing viene incluido en el JDK.
// - Toda la lógica de negocio se delega en `Sistema`, `Validaciones` y las
//   clases del dominio. Esta interfaz solo lee campos, llama a esas funciones
//   y muestra el `Resultado` (Exito -> actualiza la pantalla, Fallo -> avisa).
// - El estado es un único `EstadoSistema` inmutable que se reemplaza por el
//   nuevo solo cuando una operación devuelve `Exito`.
// - Hay dos roles. Al entrar, cada persona ve solo las pestañas de su rol:
//     Organizador  -> registra videojuegos, crea y administra SUS torneos, registra resultados.
//     Participante -> consulta torneos y se une solo o en equipo.
//   Los permisos los valida `Sistema` (la interfaz solo oculta lo que no corresponde).
//
// Rediseño visual: tema oscuro "esports" (objeto `Tema`), botones con degradado,
// campos con foco neón, tablas con filas alternadas, pestañas y barras de
// desplazamiento propias, cabecera con logotipo VEXORA y tarjetas de estadísticas.
//
// Para ejecutarla: InterfazGrafica (objeto con el método main)
// =====================================================================

import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.basic.{BasicArrowButton, BasicComboBoxUI, BasicScrollBarUI, BasicTabbedPaneUI}
import javax.swing.table.{DefaultTableCellRenderer, DefaultTableModel}
import java.awt.{BasicStroke, BorderLayout, CardLayout, Color, Component, Cursor, Dimension, FlowLayout, Font, FontMetrics, GradientPaint, Graphics, Graphics2D, GraphicsEnvironment, GridBagConstraints, GridBagLayout, GridLayout, Image, Insets, RadialGradientPaint, Rectangle, RenderingHints}
import java.awt.event.{FocusAdapter, FocusEvent, MouseAdapter, MouseEvent}
import java.awt.geom.{Path2D, Point2D}
import java.awt.image.BufferedImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*
import scala.util.Try
import Ui.Opcion

// ---------------------------------------------------------------------
// Tema visual de VEXORA (paleta, tipografía e instalación de valores globales)
// ---------------------------------------------------------------------
object Tema {
  val Fondo      = new Color(0x080B14)
  val Panel      = new Color(0x0F1424)
  val Tarjeta    = new Color(0x171E36)
  val Borde      = new Color(0x2B3560)
  val Seleccion  = new Color(0x27346A)
  val Texto      = new Color(0xE9EDFB)
  val TextoSuave = new Color(0x8E99C0)
  val Cian       = new Color(0x22D3EE)
  val Violeta    = new Color(0x8B5CF6)
  val Magenta    = new Color(0xF43F8E)
  val Verde      = new Color(0x34D399)
  val Ambar      = new Color(0xFBBF24)
  val Rojo       = new Color(0xFB7185)

  def conAlfa(c: Color, alfa: Int): Color = new Color(c.getRed, c.getGreen, c.getBlue, alfa)

  // Primera tipografía moderna disponible en el sistema; si no hay, la genérica de Java.
  val familia: String =
    List("Segoe UI", "Inter", "Helvetica Neue", "Ubuntu", "Roboto")
      .find(f => GraphicsEnvironment.getLocalGraphicsEnvironment.getAvailableFontFamilyNames.contains(f))
      .getOrElse("SansSerif")

  def fuente(estilo: Int, tamano: Float): Font = new Font(familia, estilo, 13).deriveFont(estilo, tamano)

  // Copia de Graphics con suavizado de bordes y de texto.
  def suave(g: Graphics): Graphics2D = {
    val g2 = g.create().asInstanceOf[Graphics2D]
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g2
  }

  // Se llama una sola vez, antes de crear cualquier ventana.
  def instalar(): Unit = {
    try UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName)
    catch case _: Exception => ()

    val base = fuente(Font.PLAIN, 13f)
    UIManager.getDefaults.keys.asScala.toList
      .collect { case k: String if k.endsWith(".font") => k }
      .foreach(k => UIManager.put(k, base))

    val colores: List[(String, Color)] = List(
      "Panel.background" -> Panel,
      "Label.foreground" -> Texto, "Label.background" -> Panel,
      "Button.background" -> Tarjeta, "Button.foreground" -> Texto,
      "TextField.background" -> Tarjeta, "TextField.foreground" -> Texto,
      "TextField.caretForeground" -> Cian,
      "TextField.selectionBackground" -> Violeta, "TextField.selectionForeground" -> Color.WHITE,
      "TextField.inactiveBackground" -> Tarjeta, "TextField.inactiveForeground" -> TextoSuave,
      "TextArea.background" -> Tarjeta, "TextArea.foreground" -> Texto,
      "TextArea.caretForeground" -> Cian,
      "TextArea.selectionBackground" -> Violeta, "TextArea.selectionForeground" -> Color.WHITE,
      "ComboBox.background" -> Tarjeta, "ComboBox.foreground" -> Texto,
      "ComboBox.selectionBackground" -> Seleccion, "ComboBox.selectionForeground" -> Texto,
      "ComboBox.disabledBackground" -> Panel, "ComboBox.disabledForeground" -> TextoSuave,
      "List.background" -> Tarjeta, "List.foreground" -> Texto,
      "List.selectionBackground" -> Seleccion, "List.selectionForeground" -> Texto,
      "Table.background" -> Tarjeta, "Table.foreground" -> Texto,
      "Table.selectionBackground" -> Seleccion, "Table.selectionForeground" -> Texto,
      "TableHeader.background" -> Fondo, "TableHeader.foreground" -> Cian,
      "RadioButton.background" -> Panel, "RadioButton.foreground" -> Texto,
      "ScrollPane.background" -> Panel, "Viewport.background" -> Tarjeta,
      "SplitPane.background" -> Panel,
      "TabbedPane.background" -> Panel, "TabbedPane.foreground" -> TextoSuave,
      "OptionPane.background" -> Panel, "OptionPane.messageForeground" -> Texto,
      "ToolTip.background" -> Tarjeta, "ToolTip.foreground" -> Texto,
      "TitledBorder.titleColor" -> Cian)
    colores.foreach((clave, color) => UIManager.put(clave, new ColorUIResource(color)))
  }
}

// ---------------------------------------------------------------------
// Componentes con aspecto propio
// ---------------------------------------------------------------------

// Botón redondeado con degradado violeta -> cian (o estilo discreto si es secundario).
class BotonVex(texto: String, secundario: Boolean) extends JButton(texto) {
  private var encima = false

  setContentAreaFilled(false)
  setFocusPainted(false)
  setBorderPainted(false)
  setOpaque(false)
  setForeground(Color.WHITE)
  setFont(Tema.fuente(Font.BOLD, 13f))
  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20))
  addMouseListener(new MouseAdapter {
    override def mouseEntered(e: MouseEvent): Unit = { encima = true; repaint() }
    override def mouseExited(e: MouseEvent): Unit = { encima = false; repaint() }
  })

  override protected def paintComponent(g: Graphics): Unit = {
    val g2 = Tema.suave(g)
    val (c1, c2) =
      if !isEnabled then (Tema.Borde, Tema.Borde)
      else if secundario then (if encima then Tema.Seleccion else Tema.Tarjeta, if encima then Tema.Seleccion else Tema.Tarjeta)
      else if encima then (Tema.Violeta.brighter, Tema.Cian.brighter)
      else (new Color(0x7C3AED), new Color(0x06B6D4))
    g2.setPaint(new GradientPaint(0f, 0f, c1, getWidth.toFloat, 0f, c2))
    g2.fillRoundRect(0, 0, getWidth - 1, getHeight - 1, 14, 14)
    if secundario then {
      g2.setColor(if encima then Tema.Cian else Tema.Borde)
      g2.drawRoundRect(0, 0, getWidth - 1, getHeight - 1, 14, 14)
    }
    g2.dispose()
    super.paintComponent(g)
  }
}

// Barra de desplazamiento delgada, sin flechas.
class ScrollVex extends BasicScrollBarUI {
  override protected def configureScrollBarColors(): Unit = {
    thumbColor = Tema.Borde
    trackColor = Tema.Panel
  }
  private def vacio(): JButton = {
    val b = new JButton
    val cero = new Dimension(0, 0)
    b.setPreferredSize(cero); b.setMinimumSize(cero); b.setMaximumSize(cero)
    b
  }
  override protected def createDecreaseButton(orientation: Int): JButton = vacio()
  override protected def createIncreaseButton(orientation: Int): JButton = vacio()
  override protected def paintTrack(g: Graphics, c: JComponent, r: Rectangle): Unit = {
    g.setColor(Tema.Panel)
    g.fillRect(r.x, r.y, r.width, r.height)
  }
  override protected def paintThumb(g: Graphics, c: JComponent, r: Rectangle): Unit =
    if !r.isEmpty && scrollbar.isEnabled then {
      val g2 = Tema.suave(g)
      g2.setColor(if isThumbRollover then Tema.Violeta else Tema.Borde)
      g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8)
      g2.dispose()
    }
}

// Pestañas planas con subrayado neón en la seleccionada.
class PestanasVex extends BasicTabbedPaneUI {
  override protected def installDefaults(): Unit = {
    super.installDefaults()
    tabInsets = new Insets(9, 20, 9, 20)
    selectedTabPadInsets = new Insets(0, 0, 0, 0)
    tabAreaInsets = new Insets(8, 12, 0, 12)
    contentBorderInsets = new Insets(0, 0, 0, 0)
  }
  override protected def paintTabBackground(g: Graphics, tabPlacement: Int, tabIndex: Int,
                                            x: Int, y: Int, w: Int, h: Int, isSelected: Boolean): Unit = {
    val g2 = Tema.suave(g)
    if isSelected then {
      g2.setColor(Tema.Tarjeta)
      g2.fillRoundRect(x + 2, y + 2, w - 4, h + 4, 12, 12)
      g2.setPaint(new GradientPaint(x.toFloat, 0f, Tema.Violeta, (x + w).toFloat, 0f, Tema.Cian))
      g2.fillRoundRect(x + 8, y + h - 3, w - 16, 3, 3, 3)
    }
    g2.dispose()
  }
  override protected def paintTabBorder(g: Graphics, tabPlacement: Int, tabIndex: Int,
                                        x: Int, y: Int, w: Int, h: Int, isSelected: Boolean): Unit = ()
  override protected def paintContentBorder(g: Graphics, tabPlacement: Int, selectedIndex: Int): Unit = ()
  override protected def paintFocusIndicator(g: Graphics, tabPlacement: Int, rects: Array[Rectangle], tabIndex: Int,
                                             iconRect: Rectangle, textRect: Rectangle, isSelected: Boolean): Unit = ()
  override protected def paintText(g: Graphics, tabPlacement: Int, font: Font, metrics: FontMetrics, tabIndex: Int,
                                   title: String, textRect: Rectangle, isSelected: Boolean): Unit = {
    val g2 = Tema.suave(g)
    g2.setFont(font)
    g2.setColor(if isSelected then Color.WHITE else Tema.TextoSuave)
    g2.drawString(title, textRect.x, textRect.y + metrics.getAscent)
    g2.dispose()
  }
}

// Combo con flecha cian y fondo oscuro.
class ComboVex extends BasicComboBoxUI {
  override protected def createArrowButton(): JButton = {
    val b = new BasicArrowButton(SwingConstants.SOUTH, Tema.Tarjeta, Tema.Tarjeta, Tema.Cian, Tema.Tarjeta)
    b.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4))
    b
  }
}

// Elementos de listas y combos.
class CeldaLista extends DefaultListCellRenderer {
  override def getListCellRendererComponent(lista: JList[?], valor: Object, indice: Int,
                                            seleccionada: Boolean, foco: Boolean): Component = {
    super.getListCellRendererComponent(lista, valor, indice, seleccionada, false)
    setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10))
    setBackground(if seleccionada then Tema.Seleccion else Tema.Tarjeta)
    setForeground(Tema.Texto)
    this
  }
}

// Celdas de tabla con filas alternadas.
class CeldaVex extends DefaultTableCellRenderer {
  override def getTableCellRendererComponent(tabla: JTable, valor: Object, seleccionada: Boolean,
                                             foco: Boolean, fila: Int, columna: Int): Component = {
    super.getTableCellRendererComponent(tabla, valor, seleccionada, false, fila, columna)
    setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10))
    setHorizontalAlignment(SwingConstants.LEFT)
    setBackground(if seleccionada then Tema.Seleccion else if fila % 2 == 0 then Tema.Tarjeta else Tema.Panel)
    setForeground(Tema.Texto)
    this
  }
}

// Columna "Estado" de los torneos: color según el estado.
class CeldaEstado extends CeldaVex {
  override def getTableCellRendererComponent(tabla: JTable, valor: Object, seleccionada: Boolean,
                                             foco: Boolean, fila: Int, columna: Int): Component = {
    super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna)
    val texto = String.valueOf(valor).toLowerCase
    setForeground(
      if texto.contains("final") then Tema.TextoSuave
      else if texto.contains("pend") then Tema.Ambar
      else Tema.Verde)
    setFont(Tema.fuente(Font.BOLD, 13f))
    this
  }
}

class CabeceraTablaVex extends DefaultTableCellRenderer {
  override def getTableCellRendererComponent(tabla: JTable, valor: Object, seleccionada: Boolean,
                                             foco: Boolean, fila: Int, columna: Int): Component = {
    super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna)
    setBackground(Tema.Fondo)
    setForeground(Tema.Cian)
    setFont(Tema.fuente(Font.BOLD, 12f))
    setHorizontalAlignment(SwingConstants.LEFT)
    setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 2, 0, Tema.Violeta),
      BorderFactory.createEmptyBorder(7, 10, 7, 10)))
    this
  }
}

// ---------------------------------------------------------------------
// Logotipo VEXORA (hexágono con una V + nombre)
// ---------------------------------------------------------------------
object LogoVexora {
  def dibujarMarca(g2: Graphics2D, x: Int, y: Int, lado: Int): Unit = {
    val hex = new Path2D.Double()
    for i <- 0 until 6 do
      val ang = math.toRadians(60.0 * i - 90.0)
      val px = x + lado / 2.0 + (lado / 2.0) * math.cos(ang)
      val py = y + lado / 2.0 + (lado / 2.0) * math.sin(ang)
      if i == 0 then hex.moveTo(px, py) else hex.lineTo(px, py)
    hex.closePath()
    g2.setPaint(new GradientPaint(x.toFloat, y.toFloat, Tema.Violeta, (x + lado).toFloat, (y + lado).toFloat, Tema.Cian))
    g2.fill(hex)
    g2.setColor(Tema.Fondo)
    g2.setStroke(new BasicStroke(lado / 7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
    val v = new Path2D.Double()
    v.moveTo(x + lado * 0.28, y + lado * 0.32)
    v.lineTo(x + lado * 0.50, y + lado * 0.70)
    v.lineTo(x + lado * 0.72, y + lado * 0.32)
    g2.draw(v)
  }

  // Icono para la barra de título de la ventana.
  def icono: Image = {
    val img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
    val g2 = Tema.suave(img.getGraphics)
    dibujarMarca(g2, 2, 2, 60)
    g2.dispose()
    img
  }
}

class LogoVexora(grande: Boolean) extends JComponent {
  private val alto = if grande then 84 else 56
  private val espacio = if grande then 6 else 4
  setPreferredSize(new Dimension(if grande then 340 else 290, alto))
  setMaximumSize(getPreferredSize)

  override protected def paintComponent(g: Graphics): Unit = {
    val g2 = Tema.suave(g)
    val lado = alto - 14
    LogoVexora.dibujarMarca(g2, 4, 7, lado)
    val x0 = lado + 18
    val base = if grande then 52 else 34
    g2.setFont(Tema.fuente(Font.BOLD, if grande then 42f else 28f))
    val fm = g2.getFontMetrics
    val nombre = "VEXORA"
    val ancho = nombre.map(ch => fm.charWidth(ch) + espacio).sum
    g2.setPaint(new GradientPaint(x0.toFloat, 0f, Color.WHITE, (x0 + ancho).toFloat, 0f, Tema.Cian))
    var px = x0
    nombre.foreach { ch =>
      g2.drawString(ch.toString, px, base)
      px += fm.charWidth(ch) + espacio
    }
    g2.setFont(Tema.fuente(Font.PLAIN, 11f))
    g2.setColor(Tema.TextoSuave)
    g2.drawString("GESTOR DE TORNEOS DE VIDEOJUEGOS", x0 + 2, base + (if grande then 18 else 15))
    g2.dispose()
  }
}

// Franja superior con degradado y línea neón.
class Cabecera extends JPanel(new BorderLayout) {
  setBorder(BorderFactory.createEmptyBorder(8, 16, 10, 16))
  override protected def paintComponent(g: Graphics): Unit = {
    val g2 = Tema.suave(g)
    g2.setPaint(new GradientPaint(0f, 0f, Tema.Fondo, getWidth.toFloat, 0f, new Color(0x1B1147)))
    g2.fillRect(0, 0, getWidth, getHeight)
    g2.setPaint(new GradientPaint(0f, 0f, Tema.Violeta, getWidth.toFloat, 0f, Tema.Cian))
    g2.fillRect(0, getHeight - 3, getWidth, 3)
    g2.dispose()
  }
}

// Tarjeta de estadística (número grande + título).
class TarjetaDato(titulo: String, acento: Color) extends JPanel(new BorderLayout) {
  private val lblValor = new JLabel("0")
  private val lblTitulo = new JLabel(titulo.toUpperCase)

  setOpaque(false)
  setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 14))
  lblValor.setFont(Tema.fuente(Font.BOLD, 28f))
  lblValor.setForeground(acento)
  lblTitulo.setFont(Tema.fuente(Font.BOLD, 11f))
  lblTitulo.setForeground(Tema.TextoSuave)
  add(lblTitulo, BorderLayout.NORTH)
  add(lblValor, BorderLayout.CENTER)

  def valor(n: Int): Unit = lblValor.setText(n.toString)

  override protected def paintComponent(g: Graphics): Unit = {
    val g2 = Tema.suave(g)
    g2.setColor(Tema.Tarjeta)
    g2.fillRoundRect(0, 0, getWidth - 1, getHeight - 1, 16, 16)
    g2.setColor(acento)
    g2.fillRoundRect(0, 12, 4, getHeight - 24, 4, 4)
    g2.setColor(Tema.Borde)
    g2.drawRoundRect(0, 0, getWidth - 1, getHeight - 1, 16, 16)
    g2.dispose()
  }
}

// ---------------------------------------------------------------------
// Validación de lo que escribe la persona (devuelve Resultado, como el resto del proyecto)
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
// Utilidades de Swing para no repetir código (aquí se aplica el estilo)
// ---------------------------------------------------------------------
object Ui {
  // Elemento de un combo: muestra `texto`, guarda `valor`. `clave` sirve para
  // conservar la selección cuando el combo se vuelve a llenar.
  case class Opcion[A](valor: A, texto: String, clave: Any = null) {
    override def toString: String = texto
    def identidad: Any = if clave == null then texto else clave
  }

  // Un torneo "terminó" cuando su estado es el de finalizado.
  def terminado(t: Torneo): Boolean = t.estado.descripcion().toLowerCase.contains("final")

  def nombrar[T <: Component](componente: T, nombre: String): T = {
    componente.setName(nombre)
    componente
  }

  def boton(texto: String, secundario: Boolean = false)(accion: => Unit): JButton = {
    val b = new BotonVex(texto, secundario)
    b.addActionListener(_ => accion)
    b
  }

  def estilizarCampo(f: JTextField): Unit = {
    val normal = BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(Tema.Borde, 1, true), BorderFactory.createEmptyBorder(5, 8, 5, 8))
    val activo = BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(Tema.Cian, 1, true), BorderFactory.createEmptyBorder(5, 8, 5, 8))
    f.setBorder(normal)
    f.setBackground(Tema.Tarjeta)
    f.setForeground(Tema.Texto)
    f.setCaretColor(Tema.Cian)
    f.setDisabledTextColor(Tema.TextoSuave)
    f.addFocusListener(new FocusAdapter {
      override def focusGained(e: FocusEvent): Unit = f.setBorder(activo)
      override def focusLost(e: FocusEvent): Unit = f.setBorder(normal)
    })
  }

  def campo(nombre: String, columnas: Int = 16): JTextField = {
    val f = nombrar(new JTextField(columnas), nombre)
    estilizarCampo(f)
    f
  }

  def margen(p: JComponent, px: Int = 14): Unit =
    p.setBorder(BorderFactory.createEmptyBorder(px, px, px, px))

  // Borde de "tarjeta": línea redondeada con el título en cian.
  def conTitulo(p: JComponent, titulo: String): Unit =
    p.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(Tema.Borde, 1, true), s"  $titulo  ",
        TitledBorder.LEFT, TitledBorder.TOP, Tema.fuente(Font.BOLD, 13f), Tema.Cian),
      BorderFactory.createEmptyBorder(6, 8, 8, 8)))

  // Etiqueta informativa (texto de ayuda dinámico).
  def info(texto: String = " "): JLabel = {
    val l = new JLabel(texto)
    l.setForeground(Tema.Cian)
    l.setFont(Tema.fuente(Font.ITALIC, 12f))
    l
  }

  def scroll(componente: Component): JScrollPane = {
    val sp = new JScrollPane(componente)
    sp.setBorder(BorderFactory.createLineBorder(Tema.Borde, 1, true))
    sp.getViewport.setBackground(Tema.Tarjeta)
    sp.getVerticalScrollBar.setUI(new ScrollVex)
    sp.getHorizontalScrollBar.setUI(new ScrollVex)
    sp.getVerticalScrollBar.setPreferredSize(new Dimension(10, 10))
    sp.getHorizontalScrollBar.setPreferredSize(new Dimension(10, 10))
    sp
  }

  def desplazable(componente: Component, ancho: Int, alto: Int): JScrollPane = {
    val sp = scroll(componente)
    sp.setPreferredSize(new Dimension(ancho, alto))
    sp
  }

  def areaTexto(nombre: String, filas: Int): JTextArea = {
    val a = nombrar(new JTextArea(filas, 30), nombre)
    a.setEditable(false)
    a.setLineWrap(true)
    a.setWrapStyleWord(true)
    a.setMargin(new Insets(8, 10, 8, 10))
    a.setBackground(Tema.Tarjeta)
    a.setForeground(Tema.Texto)
    a.setCaretColor(Tema.Cian)
    a
  }

  def estilizarTabla(t: JTable): Unit = {
    t.setRowHeight(30)
    t.setShowGrid(false)
    t.setIntercellSpacing(new Dimension(0, 0))
    t.setFillsViewportHeight(true)
    t.setBackground(Tema.Tarjeta)
    t.setForeground(Tema.Texto)
    t.setSelectionBackground(Tema.Seleccion)
    t.setSelectionForeground(Tema.Texto)
    t.setDefaultRenderer(classOf[Object], new CeldaVex)
    t.setDefaultRenderer(classOf[Number], new CeldaVex)
    t.getTableHeader.setDefaultRenderer(new CabeceraTablaVex)
    t.getTableHeader.setReorderingAllowed(false)
  }

  def tablaSoloLectura(nombre: String, columnas: String*): (JTable, DefaultTableModel) = {
    val modelo = new DefaultTableModel(columnas.toArray[AnyRef], 0) {
      override def isCellEditable(fila: Int, columna: Int): Boolean = false
    }
    val t = nombrar(new JTable(modelo), nombre)
    estilizarTabla(t)
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
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

  def combo[A](nombre: String): JComboBox[Opcion[A]] = {
    val cb = nombrar(new JComboBox[Opcion[A]](), nombre)
    cb.setUI(new ComboVex)
    cb.setRenderer(new CeldaLista)
    cb.setBackground(Tema.Tarjeta)
    cb.setForeground(Tema.Texto)
    cb.setBorder(BorderFactory.createLineBorder(Tema.Borde, 1, true))
    cb.setMaximumRowCount(10)
    cb
  }

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
      val etiqueta = new JLabel(texto)
      etiqueta.setForeground(Tema.TextoSuave)
      val izq = new GridBagConstraints()
      izq.gridx = bloque * 2; izq.gridy = renglon; izq.anchor = GridBagConstraints.WEST
      izq.insets = new Insets(5, if bloque == 0 then 4 else 18, 5, 10)
      panel.add(etiqueta, izq)
      val der = new GridBagConstraints()
      der.gridx = bloque * 2 + 1; der.gridy = renglon; der.fill = GridBagConstraints.HORIZONTAL
      der.weightx = 1.0; der.insets = new Insets(5, 0, 5, 4)
      panel.add(componente, der)
    }
    panel
  }

  def fila(componentes: Component*): JPanel = {
    val p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6))
    componentes.foreach(c => p.add(c))
    p
  }
}

// ---------------------------------------------------------------------
// Controlador: guarda el EstadoSistema y avisa a las pantallas cuando cambia
// ---------------------------------------------------------------------
// Partida registrada desde la interfaz, junto con quién la ganó (None = empate).
case class RegistroPartida(partida: Partida, ganador: Option[Equipo])

class Controlador {
  private var estadoActual: EstadoSistema = Sistema.estadoInicial
  private var observadores: List[() => Unit] = Nil
  var ventana: Component = null // para centrar los diálogos
  var alMensaje: (String, Boolean) => Unit = (_, _) => ()

  // Rondas y partidas viven en la interfaz (EstadoSistema todavía no las guarda).
  // Están aquí, y no dentro de cada panel, para que la pestaña de resumen también las lea.
  var rondas: Map[Int, List[List[DesempenoEquipo]]] = Map.empty
  var partidas: Map[Int, List[RegistroPartida]] = Map.empty

  def estado: EstadoSistema = estadoActual

  // ----- Sesión: se guarda solo el id; el usuario se busca siempre en el estado vigente -----
  private var sesion: Option[Int] = None
  def idSesion: Int = sesion.getOrElse(-1)
  def usuarioActual: Option[Usuario] =
    sesion.flatMap(id => estadoActual.usuarios.filter(u => u.id == id).headOption)
  def iniciarSesion(idUsuario: Int): Unit = sesion = Some(idUsuario)
  def cerrarSesion(): Unit = sesion = None

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

  // Igual que `ejecutar`, pero la operación recibe el id del usuario con sesión iniciada.
  // `Sistema` decide si ese usuario tiene el rol necesario.
  def ejecutarComo(operacion: (EstadoSistema, Int) => Resultado[EstadoSistema], mensajeExito: String): Boolean =
    sesion match
      case Some(idUsuario) => ejecutar(e => operacion(e, idUsuario), mensajeExito)
      case None =>
        error("Inicia sesión para continuar")
        false
}

// ---------------------------------------------------------------------
// Videojuegos (organizador)
// ---------------------------------------------------------------------
class PanelVideojuegos(c: Controlador) extends JPanel(new BorderLayout(10, 10)) {
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
        if c.ejecutarComo((e, idUsuario) => Sistema.registrarVideojuegoComo(e, idUsuario, videojuego), s"Videojuego '$titulo' registrado") then {
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

  private val listado = new JPanel(new BorderLayout())
  listado.add(Ui.scroll(tabla), BorderLayout.CENTER)
  Ui.conTitulo(listado, "Catálogo de videojuegos")

  add(panelForm, BorderLayout.NORTH)
  add(listado, BorderLayout.CENTER)

  txtGenero.addActionListener(_ => registrar())
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
// Torneos. Con `gestionar = true` (organizador): crear, iniciar y finalizar SUS torneos.
// Con `gestionar = false` (participante): solo consulta.
// ---------------------------------------------------------------------
class PanelTorneos(c: Controlador, gestionar: Boolean) extends JPanel(new BorderLayout(10, 10)) {
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
  private val lblRequisito = Ui.info()
  private val cbFiltro = Ui.combo[EstadoSistema => List[Torneo]]("cbFiltroTorneos")
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaTorneos",
    "ID", "Torneo", "Tipo", "Videojuego", "Modalidad", "Estado", "Equipos", "Organizador")
  private val txtDetalle = Ui.areaTexto("txtDetalleTorneo", 5)
  private var visibles: List[Torneo] = Nil

  tabla.getColumnModel.getColumn(5).setCellRenderer(new CeldaEstado) // color según el estado

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
        if c.ejecutarComo((e, idUsuario) => Sistema.crearTorneo(e, idUsuario, torneo), s"Torneo '${torneo.nombreTorneo}' creado (pendiente)") then
          txtNombre.setText("")
  }

  private def seleccionado: Option[Torneo] =
    Ui.filaSeleccionada(tabla).flatMap(i => visibles.lift(i))

  // Función de orden superior: la operación (iniciar o finalizar) la elige quien llama.
  // Solo funciona si el torneo es del organizador con sesión iniciada.
  private def administrar(operacion: (EstadoSistema, Int, Int) => Resultado[EstadoSistema], mensaje: String): Unit =
    seleccionado match
      case None => c.error("Selecciona un torneo de la tabla")
      case Some(t) =>
        c.ejecutarComo((e, idUsuario) => operacion(e, idUsuario, t.idTorneo), s"${t.nombreTorneo}: $mensaje")

  private def finalizar(): Unit =
    seleccionado match
      case None => c.error("Selecciona un torneo de la tabla")
      case Some(t) =>
        if c.confirmar(s"¿Finalizar el torneo '${t.nombreTorneo}'? Esta acción no se puede deshacer.") then
          administrar(Sistema.finalizarTorneo, "torneo finalizado")

  private def detalle(t: Torneo): String = {
    val extra = t match
      case p: TorneoSistemaDePuntos =>
        s"\nPeso de clasificación: ${p.pesoClasificacion}\nPuntaje por baja: ${p.puntajePorBaja}"
      case _ => ""
    val equipos =
      if t.equipos.isEmpty then "  (todavía no hay equipos inscritos)"
      else t.equipos.map(e => s"  - ${e.nombreEquipo} (${e.integrantes.length} integrante(s))").mkString("\n")
    val organizador = Sistema.nombreOrganizador(c.estado, t.idTorneo)
    s"${t.descripción()}$extra\nOrganizador: $organizador\nRequisito: ${t.modalidad.requisito()}\n\nEquipos inscritos (${t.equipos.length}):\n$equipos"
  }

  private def mostrarDetalle(): Unit = {
    txtDetalle.setText(seleccionado.map(t => detalle(t)).getOrElse("Selecciona un torneo de la tabla para ver su detalle."))
    txtDetalle.setCaretPosition(0)
  }

  private def refrescar(): Unit = {
    val estado = c.estado
    Ui.rellenar(cbJuego, estado.videojuegos.map(v => Opcion(v, s"${v.titulo} (${v.genero})", v.idJuego)))

    // Solo el organizador tiene "Mis torneos".
    val propios: List[Opcion[EstadoSistema => List[Torneo]]] =
      if gestionar then
        List(Opcion[EstadoSistema => List[Torneo]]((e: EstadoSistema) => Sistema.torneosDeOrganizador(e, c.idSesion), "Mis torneos", "mios"))
      else Nil

    val filtros: List[Opcion[EstadoSistema => List[Torneo]]] =
      List(
        Opcion[EstadoSistema => List[Torneo]]((e: EstadoSistema) => e.torneos, "Todos los torneos", "todos"),
        Opcion[EstadoSistema => List[Torneo]]((e: EstadoSistema) => Sistema.torneosActivos(e), "Solo torneos activos", "activos"),
        Opcion[EstadoSistema => List[Torneo]]((e: EstadoSistema) => Sistema.filtrarTorneos(e, t => t.estado == TorneoPendiente), "Pendientes (abiertos a inscripción)", "pendientes")) ++
        propios ++
        estado.videojuegos.map(v =>
          Opcion[EstadoSistema => List[Torneo]]((e: EstadoSistema) => Sistema.torneosPorVideojuego(e, v.idJuego), s"Videojuego: ${v.titulo}", s"juego-${v.idJuego}"))
    Ui.rellenar(cbFiltro, filtros)

    val idPrevio = seleccionado.map(_.idTorneo)
    visibles = Ui.seleccion(cbFiltro).map(f => f(estado)).getOrElse(estado.torneos)
    Ui.llenar(modelo, visibles.map(t => {
      val r = Sistema.resumir(t)
      List(r.id, r.nombre, r.tipo, r.videojuego, r.modalidad, r.estado, r.cantidadEquipos, Sistema.nombreOrganizador(estado, r.id))
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

  private val barra =
    if gestionar then Ui.fila(
      new JLabel("Mostrar:"), cbFiltro,
      Ui.boton("Iniciar torneo")(administrar(Sistema.iniciarTorneo, "torneo iniciado")),
      Ui.boton("Finalizar torneo", true)(finalizar()))
    else Ui.fila(new JLabel("Mostrar:"), cbFiltro)

  private val centro = new JPanel(new BorderLayout(6, 6))
  centro.add(barra, BorderLayout.NORTH)
  centro.add(Ui.scroll(tabla), BorderLayout.CENTER)
  centro.add(Ui.desplazable(txtDetalle, 400, 120), BorderLayout.SOUTH)

  if gestionar then add(panelNuevo, BorderLayout.NORTH)
  add(centro, BorderLayout.CENTER)

  cbTipo.addActionListener(_ => actualizarFormulario())
  cbModalidad.addActionListener(_ => actualizarFormulario())
  cbFiltro.addActionListener(_ => if cbFiltro.getItemCount > 0 then refrescar())
  Ui.alSeleccionar(tabla)(mostrarDetalle())
  actualizarFormulario()
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
// Inscripciones (organizador, solo lectura): equipos inscritos en SUS torneos.
// Las personas se inscriben desde la pestaña "Unirme a un torneo" (participantes).
// ---------------------------------------------------------------------
class PanelInscripciones(c: Controlador) extends JPanel(new BorderLayout(10, 10)) {
  private val cbTorneo = Ui.combo[Torneo]("cbTorneoInscripcion")
  private val lblInfo = Ui.info()
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaInscritos", "ID", "Equipo", "Integrantes", "Nicknames")

  private def refrescarDetalle(): Unit =
    Ui.seleccion(cbTorneo) match
      case None =>
        lblInfo.setText("Todavía no has creado torneos (pestaña Torneos).")
        Ui.llenar(modelo, Nil)
      case Some(t) =>
        lblInfo.setText(s"Estado: ${t.estado.descripcion()} | Modalidad ${t.modalidad.descripcion()}: ${t.modalidad.requisito()}")
        Ui.llenar(modelo, t.equipos.map(e =>
          List(e.idEquipo, e.nombreEquipo, e.integrantes.length, e.integrantes.map(i => i.nickname).mkString(", "))))

  private def refrescar(): Unit = {
    Ui.rellenar(cbTorneo, Sistema.torneosDeOrganizador(c.estado, c.idSesion).map(t =>
      Opcion(t, s"#${t.idTorneo} ${t.nombreTorneo} [${t.estado.descripcion()}]", t.idTorneo)))
    refrescarDetalle()
  }

  Ui.margen(this)
  private val panelForm = new JPanel(new BorderLayout())
  panelForm.add(Ui.formulario(List(("Torneo:", cbTorneo))), BorderLayout.CENTER)
  panelForm.add(lblInfo, BorderLayout.SOUTH)
  Ui.conTitulo(panelForm, "Mis torneos")

  private val listado = new JPanel(new BorderLayout())
  listado.add(Ui.scroll(tabla), BorderLayout.CENTER)
  Ui.conTitulo(listado, "Equipos inscritos en el torneo seleccionado")

  add(panelForm, BorderLayout.NORTH)
  add(listado, BorderLayout.CENTER)

  cbTorneo.addActionListener(_ => refrescarDetalle())
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
// Puntuación (organizador): torneos por sistema de puntos -> TablaPosiciones
// ---------------------------------------------------------------------
class PanelPuntuacion(c: Controlador) extends JPanel(new BorderLayout(10, 10)) {
  private val cbTorneo = Ui.combo[TorneoSistemaDePuntos]("cbTorneoPuntos")
  private val lblFormula = Ui.info()
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
  Ui.estilizarTabla(tablaRonda)

  // Rondas ya registradas por torneo (en la interfaz; el cálculo lo hace TorneoSistemaDePuntos).
  private def rondasDe(idTorneo: Int): List[List[DesempenoEquipo]] = c.rondas.getOrElse(idTorneo, Nil)

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
      s"Puntos por ronda = (equipos − puesto + 1) × ${t.pesoClasificacion} + bajas × ${t.puntajePorBaja}  |  ${t.equipos.length} equipo(s) inscrito(s)" +
      (if Ui.terminado(t) then "  |  FINALIZADO: ya no admite más rondas" else "")
    ).getOrElse("No hay torneos por sistema de puntos. Crea uno en la pestaña Torneos."))
    txtRondas.setText(textoRondas())
    cargarRonda()
    limpiarResultados()
  }

  private def agregarRonda(): Unit =
    Ui.seleccion(cbTorneo) match
      case None => c.error("No hay un torneo por sistema de puntos seleccionado")
      case Some(t) if Ui.terminado(t) => c.error("El torneo ya finalizó: no se pueden agregar más rondas")
      case Some(t) =>
        leerRonda(t).encadenar(ronda => t.validarRonda(ronda)) match
          case Fallo(motivo) => c.error(motivo)
          case Exito(ronda) =>
            c.rondas = c.rondas.updated(t.idTorneo, rondasDe(t.idTorneo) :+ ronda)
            c.avisar(s"Ronda ${rondasDe(t.idTorneo).length} registrada")
            alCambiarTorneo()

  private def quitarRonda(): Unit =
    Ui.seleccion(cbTorneo).foreach(t =>
      if Ui.terminado(t) then c.error("El torneo ya finalizó: no se pueden modificar sus rondas")
      else {
        c.rondas = c.rondas.updated(t.idTorneo, rondasDe(t.idTorneo).dropRight(1))
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
        val podio = ordenados.take(3).zipWithIndex.map((r, i) => s"${i + 1}.º ${r.descripcionResultado()}")
        txtPodio.setText(s"Podio (${tabla.torneo.nombreTorneo}, $fecha)\n" + podio.mkString("\n"))
        c.avisar("Tabla de posiciones calculada")

  private def refrescar(): Unit = {
    val torneos = Sistema.torneosDeOrganizador(c.estado, c.idSesion).collect { case t: TorneoSistemaDePuntos => t }
    Ui.rellenar(cbTorneo, torneos.map(t => Opcion(t, s"#${t.idTorneo} ${t.nombreTorneo} [${t.estado.descripcion()}]", t.idTorneo)))
    alCambiarTorneo()
  }

  Ui.margen(this)
  txtFecha.setText(LocalDate.now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))

  private val arriba = new JPanel(new BorderLayout())
  arriba.add(Ui.formulario(List(("Torneo:", cbTorneo))), BorderLayout.CENTER)
  arriba.add(lblFormula, BorderLayout.SOUTH)
  Ui.conTitulo(arriba, "Torneo por sistema de puntos")

  private val izquierda = new JPanel(new BorderLayout(6, 6))
  private val ayuda = new JLabel("Edita la posición y las bajas de cada equipo y pulsa «Agregar ronda».")
  ayuda.setForeground(Tema.TextoSuave)
  izquierda.add(ayuda, BorderLayout.NORTH)
  izquierda.add(Ui.scroll(tablaRonda), BorderLayout.CENTER)
  private val sur = new JPanel(new BorderLayout(6, 6))
  sur.add(Ui.fila(Ui.boton("Agregar ronda")(agregarRonda()), Ui.boton("Quitar última ronda", true)(quitarRonda())), BorderLayout.NORTH)
  sur.add(Ui.desplazable(txtRondas, 300, 100), BorderLayout.CENTER)
  izquierda.add(sur, BorderLayout.SOUTH)
  Ui.conTitulo(izquierda, "1. Rondas")

  private val derecha = new JPanel(new BorderLayout(6, 6))
  derecha.add(Ui.fila(new JLabel("Fecha:"), txtFecha, Ui.boton("Calcular tabla de posiciones")(calcular())), BorderLayout.NORTH)
  derecha.add(Ui.scroll(tablaResultados), BorderLayout.CENTER)
  derecha.add(Ui.desplazable(txtPodio, 300, 100), BorderLayout.SOUTH)
  Ui.conTitulo(derecha, "2. Tabla de posiciones")

  private val division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha)
  division.setResizeWeight(0.5)
  division.setBorder(null)
  division.setDividerSize(8)

  add(arriba, BorderLayout.NORTH)
  add(division, BorderLayout.CENTER)

  cbTorneo.addActionListener(_ => alCambiarTorneo())
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
// Partidas (organizador): PartidaPuntos y PartidaEliminacion
// Las partidas viven en la interfaz: el EstadoSistema del proyecto aún no las guarda.
// ---------------------------------------------------------------------
class PanelPartidas(c: Controlador) extends JPanel(new BorderLayout(10, 10)) {
  private val cbTorneo = Ui.combo[Torneo]("cbTorneoPartida")
  private val cbE1 = Ui.combo[Equipo]("cbEquipo1")
  private val cbE2 = Ui.combo[Equipo]("cbEquipo2")
  private val txtP1 = Ui.campo("txtPuntaje1", 6)
  private val txtP2 = Ui.campo("txtPuntaje2", 6)
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaPartidas", "ID", "Equipo 1", "Equipo 2", "Resultado")
  private val tarjetas = new CardLayout
  private val panelAccion = new JPanel(tarjetas)

  private def partidasDe(idTorneo: Int): List[Partida] = c.partidas.getOrElse(idTorneo, Nil).map(r => r.partida)

  private def refrescarTabla(): Unit =
    Ui.llenar(modelo, Ui.seleccion(cbTorneo).map(t => partidasDe(t.idTorneo)).getOrElse(Nil)
      .map(p => List(p.idPartida, p.equipo1.nombreEquipo, p.equipo2.nombreEquipo, p.resumen())))

  // Crea la partida con la función recibida; si sale bien la agrega a la lista del torneo.
  private def agregarPartida(construir: (Int, Equipo, Equipo) => Resultado[Partida],
                             ganadorDe: (Equipo, Equipo) => Option[Equipo]): Unit =
    (Ui.seleccion(cbTorneo), Ui.seleccion(cbE1), Ui.seleccion(cbE2)) match
      case (Some(t), Some(e1), Some(e2)) =>
        if Ui.terminado(t) then c.error("El torneo ya finalizó: no se pueden agregar más partidas")
        else if e1.idEquipo == e2.idEquipo then c.error("Elige dos equipos distintos")
        else
          val id = Sistema.siguienteId(partidasDe(t.idTorneo), (p: Partida) => p.idPartida)
          construir(id, e1, e2) match
            case Fallo(motivo) => c.error(motivo)
            case Exito(partida) =>
              c.partidas = c.partidas.updated(t.idTorneo,
                c.partidas.getOrElse(t.idTorneo, Nil) :+ RegistroPartida(partida, ganadorDe(e1, e2)))
              refrescarTabla()
              c.avisar(s"Partida ${partida.idPartida} registrada: ${partida.resumen()}")
      case _ => c.error("Elige un torneo con al menos 2 equipos inscritos")

  // Solo se llama cuando los puntajes ya se validaron; si empatan no hay ganador.
  private def ganadorPorPuntos(e1: Equipo, e2: Equipo): Option[Equipo] =
    (Try(txtP1.getText.trim.toInt).toOption, Try(txtP2.getText.trim.toInt).toOption) match
      case (Some(a), Some(b)) if a > b => Some(e1)
      case (Some(a), Some(b)) if b > a => Some(e2)
      case _                           => None

  private def registrarPuntos(): Unit =
    agregarPartida((id, e1, e2) =>
      Entradas.combinar(
        Entradas.entero(txtP1.getText, "Puntaje del equipo 1"),
        Entradas.entero(txtP2.getText, "Puntaje del equipo 2")).encadenar((p1, p2) =>
        PartidaPuntos(0, 0, id, e1, e2, EnCurso).actualizarPuntaje(p1, p2).mapear[Partida](p => p)),
      (e1, e2) => ganadorPorPuntos(e1, e2))

  private def registrarGanador(estadoFinal: EstadoPartida): Unit =
    agregarPartida((id, e1, e2) =>
      Exito[Partida](PartidaEliminacion(id, e1, e2, EnCurso).actualizarEstadoPartida(estadoFinal)),
      (e1, e2) => Some(if estadoFinal == FinalizadoGanoEquipo1 then e1 else e2))

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
    // Solo eliminación directa / doble eliminación (los de puntos usan rondas) y que no hayan finalizado.
    val torneos = Sistema.filtrarTorneos(c.estado, t =>
      !t.isInstanceOf[TorneoSistemaDePuntos] && !Ui.terminado(t) &&
        t.equipos.length >= 2 && Sistema.esDuenoDeTorneo(c.estado, c.idSesion, t.idTorneo))
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
  Ui.conTitulo(form, "Registrar una partida (torneos de eliminación en curso con al menos 2 equipos)")

  private val listado = new JPanel(new BorderLayout())
  listado.add(Ui.scroll(tabla), BorderLayout.CENTER)
  Ui.conTitulo(listado, "Partidas del torneo seleccionado")

  add(form, BorderLayout.NORTH)
  add(listado, BorderLayout.CENTER)

  cbTorneo.addActionListener(_ => alCambiarTorneo())
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
// Acceso: iniciar sesión (por correo) o crear una cuenta de organizador / participante
// ---------------------------------------------------------------------
class PanelAcceso(c: Controlador, alEntrar: Usuario => Unit, cargarEjemplo: () => Unit)
  extends JPanel(new GridBagLayout) {

  private val txtCorreoLogin = Ui.campo("txtCorreoLogin", 22)
  private val cbRol = Ui.combo[Boolean]("cbRolNuevo") // true = organizador
  private val txtNombre = Ui.campo("txtNombreUsuario", 22)
  private val txtCorreoNuevo = Ui.campo("txtCorreoNuevo", 22)
  private val txtNick = Ui.campo("txtNickUsuario", 22)

  cbRol.addItem(Opcion(false, "Participante"))
  cbRol.addItem(Opcion(true, "Organizador"))

  private def esOrganizador: Boolean = Ui.seleccion(cbRol).getOrElse(false)

  private def entrar(correo: String): Unit =
    Sistema.iniciarSesion(c.estado, correo) match
      case Fallo(motivo) => c.error(motivo)
      case Exito(usuario) =>
        txtCorreoLogin.setText("")
        alEntrar(usuario)

  private def iniciarSesion(): Unit =
    Entradas.requerido(txtCorreoLogin.getText, "Correo") match
      case Fallo(motivo) => c.error(motivo)
      case Exito(correo) => entrar(correo)

  private def crearCuenta(): Unit = {
    val organizador = esOrganizador
    val datos: Resultado[(String, (String, String))] =
      Entradas.combinar(
        Entradas.requerido(txtNombre.getText, "Nombre"),
        Entradas.combinar(
          Entradas.correo(txtCorreoNuevo.getText),
          if organizador then Exito[String]("") else Entradas.requerido(txtNick.getText, "Nickname")))

    datos match
      case Fallo(motivo) => c.error(motivo)
      case Exito((nombre, (correo, nick))) =>
        val operacion: EstadoSistema => Resultado[EstadoSistema] =
          if organizador then ((e: EstadoSistema) => Sistema.registrarOrganizador(e, nombre, correo))
          else ((e: EstadoSistema) => Sistema.registrarParticipante(e, nick, nombre, correo))
        if c.ejecutar(operacion, s"Cuenta creada para $nombre") then {
          List(txtNombre, txtCorreoNuevo, txtNick).foreach(t => t.setText(""))
          entrar(correo)
        }
  }

  setBackground(Tema.Fondo)

  // Resplandores de colores de fondo (puro adorno, no afecta a la lógica).
  override protected def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)
    val g2 = Tema.suave(g)
    def brillo(cx: Int, cy: Int, radio: Int, color: Color): Unit = {
      g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx.toFloat, cy.toFloat), radio.toFloat,
        Array(0f, 1f), Array(Tema.conAlfa(color, 90), Tema.conAlfa(color, 0))))
      g2.fillRect(0, 0, getWidth, getHeight)
    }
    brillo(getWidth / 5, getHeight / 4, 420, Tema.Violeta)
    brillo(getWidth * 4 / 5, getHeight * 3 / 4, 460, Tema.Cian)
    brillo(getWidth / 2, getHeight, 360, Tema.Magenta)
    g2.dispose()
  }

  private val ingreso = new JPanel(new BorderLayout())
  ingreso.add(Ui.formulario(List(("Correo:", txtCorreoLogin))), BorderLayout.CENTER)
  ingreso.add(Ui.fila(Ui.boton("Entrar")(iniciarSesion())), BorderLayout.SOUTH)
  Ui.conTitulo(ingreso, "Iniciar sesión")

  private val registro = new JPanel(new BorderLayout())
  registro.add(Ui.formulario(List(
    ("Rol:", cbRol), ("Nombre:", txtNombre), ("Correo:", txtCorreoNuevo), ("Nickname:", txtNick))), BorderLayout.CENTER)
  registro.add(Ui.fila(Ui.boton("Crear cuenta")(crearCuenta())), BorderLayout.SOUTH)
  Ui.conTitulo(registro, "Crear una cuenta")

  private val ejemplo = new JPanel(new BorderLayout())
  ejemplo.add(Ui.fila(Ui.nombrar(Ui.boton("Cargar datos de ejemplo", true)(cargarEjemplo()), "btnEjemplo")), BorderLayout.NORTH)
  private val pista = new JLabel("<html>Con los datos de ejemplo entras con: <b>organizador@gmail.com</b> (organizador) " +
    "o <b>a@gmail.com</b> ... <b>f@gmail.com</b> (participantes).</html>")
  pista.setForeground(Tema.TextoSuave)
  ejemplo.add(pista, BorderLayout.CENTER)
  Ui.conTitulo(ejemplo, "¿Primera vez?")

  private val logo = new LogoVexora(true)
  private val titulo = new JLabel("Bienvenido a VEXORA")
  titulo.setFont(Tema.fuente(Font.BOLD, 20f))
  titulo.setForeground(Tema.Texto)
  private val subtitulo = new JLabel("Organiza torneos, arma tus equipos y sigue cada posición.")
  subtitulo.setForeground(Tema.TextoSuave)

  // Cada sección usa su altura natural (antes una cuadrícula fija recortaba el formulario).
  private val contenido = new JPanel()
  contenido.setOpaque(false)
  contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS))
  List[JComponent](logo, titulo, subtitulo, ingreso, registro, ejemplo).foreach(componente => {
    componente.setAlignmentX(Component.LEFT_ALIGNMENT)
    contenido.add(componente)
    contenido.add(Box.createVerticalStrut(10))
  })
  contenido.setPreferredSize(new Dimension(480, contenido.getPreferredSize.height))
  add(contenido)

  cbRol.addActionListener(_ => txtNick.setEnabled(!esOrganizador))
}

// ---------------------------------------------------------------------
// Unirme a un torneo (participante): solo, con un equipo mío o con un equipo nuevo
// ---------------------------------------------------------------------
class PanelUnirse(c: Controlador) extends JPanel(new BorderLayout(10, 10)) {
  private val cbTorneo = Ui.combo[Torneo]("cbTorneoUnirse")
  private val lblInfo = Ui.info()
  private val rbSolo = Ui.nombrar(new JRadioButton("Solo (con un equipo de un integrante)"), "rbSolo")
  private val rbEquipo = Ui.nombrar(new JRadioButton("Con uno de mis equipos:"), "rbEquipoExistente")
  private val rbNuevo = Ui.nombrar(new JRadioButton("Con un equipo nuevo. Nombre:"), "rbEquipoNuevo")
  private val cbEquipo = Ui.combo[Equipo]("cbMisEquipos")
  private val txtEquipo = Ui.campo("txtEquipoNuevo", 20)
  private val modeloCompaneros = new DefaultListModel[Opcion[Participante]]()
  private val listaCompaneros = Ui.nombrar(new JList[Opcion[Participante]](modeloCompaneros), "listaCompaneros")
  listaCompaneros.setCellRenderer(new CeldaLista)
  listaCompaneros.setBackground(Tema.Tarjeta)

  private def yo: Option[Participante] =
    c.usuarioActual.collect { case p: Participante => p }

  private def companerosElegidos: List[Participante] =
    listaCompaneros.getSelectedValuesList.asScala.toList.map(o => o.valor)

  private def actualizarModo(): Unit = {
    cbEquipo.setEnabled(rbEquipo.isSelected)
    txtEquipo.setEnabled(rbNuevo.isSelected)
    listaCompaneros.setEnabled(rbNuevo.isSelected)
  }

  private def mostrarInfo(): Unit =
    lblInfo.setText(Ui.seleccion(cbTorneo).map(t =>
      s"Modalidad ${t.modalidad.descripcion()}: ${t.modalidad.requisito()}  |  ${t.equipos.length} equipo(s) inscrito(s)"
    ).getOrElse("No hay torneos pendientes abiertos a inscripción."))

  private def unirse(): Unit =
    Entradas.presente(Ui.seleccion(cbTorneo), "No hay torneos pendientes para unirse") match
      case Fallo(motivo) => c.error(motivo)
      case Exito(torneo) =>
        val mensaje = s"Te inscribiste en '${torneo.nombreTorneo}'"
        if rbEquipo.isSelected then
          Entradas.presente(Ui.seleccion(cbEquipo), "Todavía no perteneces a ningún equipo") match
            case Fallo(motivo) => c.error(motivo)
            case Exito(equipo) =>
              c.ejecutarComo((e, idUsuario) => Sistema.unirseConEquipo(e, idUsuario, torneo.idTorneo, equipo.idEquipo), mensaje)
        else if rbNuevo.isSelected then {
          val ids = companerosElegidos.map(p => p.idParticipante)
          if c.ejecutarComo((e, idUsuario) =>
            Sistema.unirseConEquipoNuevo(e, idUsuario, torneo.idTorneo, txtEquipo.getText, ids), mensaje) then
            txtEquipo.setText("")
        }
        else
          c.ejecutarComo((e, idUsuario) => Sistema.unirseSolo(e, idUsuario, torneo.idTorneo), mensaje)

  private def refrescar(): Unit = {
    val estado = c.estado
    Ui.rellenar(cbTorneo, Sistema.filtrarTorneos(estado, t => t.estado == TorneoPendiente).map(t =>
      Opcion(t, s"#${t.idTorneo} ${t.nombreTorneo} [${Sistema.tipoDeTorneo(t)} - ${t.modalidad.descripcion()}]", t.idTorneo)))
    Ui.rellenar(cbEquipo, yo.map(p => p.misEquipos(estado)).getOrElse(Nil).map(e =>
      Opcion(e, s"#${e.idEquipo} ${e.nombreEquipo} (${e.integrantes.length} integrante(s))", e.idEquipo)))

    // Compañeros posibles: los demás participantes registrados (se conserva la selección).
    val previos = companerosElegidos.map(p => p.idParticipante)
    val posibles = Sistema.participantes(estado).filter(p => yo.map(m => m.idParticipante != p.idParticipante).getOrElse(true))
      .map(p => Opcion(p, s"${p.nickname} - ${p.nombre}", p.idParticipante))
    modeloCompaneros.clear()
    posibles.foreach(o => modeloCompaneros.addElement(o))
    val indices = posibles.zipWithIndex.filter((o, i) => previos.contains(o.valor.idParticipante)).map((o, i) => i)
    listaCompaneros.setSelectedIndices(indices.toArray)
    mostrarInfo()
  }

  Ui.margen(this)

  private val grupo = new ButtonGroup()
  List(rbSolo, rbEquipo, rbNuevo).foreach(r => grupo.add(r))
  rbSolo.setSelected(true)

  private val nuevo = new JPanel(new BorderLayout(6, 6))
  nuevo.add(Ui.fila(rbNuevo, txtEquipo), BorderLayout.NORTH)
  private val panelLista = Ui.desplazable(listaCompaneros, 320, 130)
  Ui.conTitulo(panelLista, "Compañeros (Ctrl o Mayús para elegir varios)")
  nuevo.add(panelLista, BorderLayout.CENTER)

  private val modos = new JPanel(new BorderLayout(6, 6))
  modos.add(Ui.fila(rbSolo), BorderLayout.NORTH)
  private val modosInferior = new JPanel(new BorderLayout(6, 6))
  modosInferior.add(Ui.fila(rbEquipo, cbEquipo), BorderLayout.NORTH)
  modosInferior.add(nuevo, BorderLayout.CENTER)
  modos.add(modosInferior, BorderLayout.CENTER)

  private val panelForm = new JPanel(new BorderLayout(6, 6))
  private val arriba = new JPanel(new BorderLayout())
  arriba.add(Ui.formulario(List(("Torneo:", cbTorneo))), BorderLayout.CENTER)
  arriba.add(lblInfo, BorderLayout.SOUTH)
  panelForm.add(arriba, BorderLayout.NORTH)
  panelForm.add(modos, BorderLayout.CENTER)
  panelForm.add(Ui.fila(Ui.boton("Unirme al torneo")(unirse())), BorderLayout.SOUTH)
  Ui.conTitulo(panelForm, "Unirme a un torneo pendiente")

  add(panelForm, BorderLayout.CENTER)

  List(rbSolo, rbEquipo, rbNuevo).foreach(r => r.addActionListener(_ => actualizarModo()))
  cbTorneo.addActionListener(_ => mostrarInfo())
  actualizarModo()
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
// Mis inscripciones (participante): torneos en los que participo y con qué equipo
// ---------------------------------------------------------------------
class PanelMisInscripciones(c: Controlador) extends JPanel(new BorderLayout(10, 10)) {
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaMisInscripciones",
    "ID", "Torneo", "Videojuego", "Estado", "Mi equipo", "Integrantes")
  tabla.getColumnModel.getColumn(3).setCellRenderer(new CeldaEstado)

  private def refrescar(): Unit = {
    val yo = c.usuarioActual.collect { case p: Participante => p }
    val filas = yo.map(p =>
      p.torneosInscritos(c.estado).flatMap(t =>
        p.equipoEn(t).map(e =>
          List(t.idTorneo, t.nombreTorneo, t.videojuego.titulo, t.estado.descripcion(), e.nombreEquipo,
            e.integrantes.map(i => i.nickname).mkString(", "))))).getOrElse(Nil)
    Ui.llenar(modelo, filas)
  }

  Ui.margen(this)
  private val listado = new JPanel(new BorderLayout())
  listado.add(Ui.scroll(tabla), BorderLayout.CENTER)
  Ui.conTitulo(listado, "Torneos en los que participo")
  add(listado, BorderLayout.CENTER)
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
// Resumen de un torneo finalizado: tarjeta dorada con el ganador, clasificación
// (sistema de puntos) o desarrollo de las partidas (eliminación) y datos del torneo.
// ---------------------------------------------------------------------
class TarjetaGanador extends JPanel(new BorderLayout) {
  private val lblRotulo = new JLabel("SIN RESULTADOS")
  private val lblNombre = new JLabel("—")
  private val lblDetalle = new JLabel(" ")

  setOpaque(false)
  setPreferredSize(new Dimension(100, 136))
  setBorder(BorderFactory.createEmptyBorder(14, 26, 14, 26))
  lblRotulo.setFont(Tema.fuente(Font.BOLD, 13f))
  lblRotulo.setForeground(Tema.Ambar)
  lblNombre.setFont(Tema.fuente(Font.BOLD, 34f))
  lblNombre.setForeground(Color.WHITE)
  lblDetalle.setFont(Tema.fuente(Font.PLAIN, 13f))
  lblDetalle.setForeground(Tema.Texto)
  private val centro = new JPanel(new GridLayout(3, 1, 0, 2))
  centro.setOpaque(false)
  List(lblRotulo, lblNombre, lblDetalle).foreach(l => centro.add(l))
  add(centro, BorderLayout.CENTER)

  def mostrar(rotulo: String, nombre: String, detalle: String): Unit = {
    lblRotulo.setText(rotulo)
    lblNombre.setText(nombre)
    lblDetalle.setText(detalle)
    repaint()
  }

  override protected def paintComponent(g: Graphics): Unit = {
    val g2 = Tema.suave(g)
    g2.setPaint(new GradientPaint(0f, 0f, Tema.conAlfa(Tema.Ambar, 80), getWidth.toFloat, 0f, Tema.Tarjeta))
    g2.fillRoundRect(0, 0, getWidth - 1, getHeight - 1, 18, 18)
    g2.setColor(Tema.Ambar)
    g2.setStroke(new BasicStroke(2f))
    g2.drawRoundRect(1, 1, getWidth - 3, getHeight - 3, 18, 18)
    g2.dispose()
  }
}

class PanelResumenTorneo(c: Controlador) extends JPanel(new BorderLayout(10, 10)) {
  // Lo que se muestra de un torneo: campeón(es), detalle, filas de la tabla y texto de datos.
  private case class Resumen(campeones: List[Equipo], detalle: String, filas: List[List[Any]], texto: String)

  private val cbTorneo = Ui.combo[Torneo]("cbTorneoResumen")
  private val lblInfo = Ui.info()
  private val tarjeta = new TarjetaGanador
  private val (tabla, modelo) = Ui.tablaSoloLectura("tablaResumen", "Posición", "Equipo / Enfrentamiento", "Detalle")
  private val txtResumen = Ui.areaTexto("txtResumenTorneo", 8)

  private def terminado(t: Torneo): Boolean = Ui.terminado(t)

  private def resumenPuntos(p: TorneoSistemaDePuntos, cabecera: String): Resumen = {
    val rondas = c.rondas.getOrElse(p.idTorneo, Nil)
    val fecha = LocalDate.now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    if rondas.isEmpty then
      Resumen(Nil, "No se registraron rondas: no hay resultados para decidir un ganador", Nil, cabecera)
    else
      Sistema.calcularTablaPuntos(c.estado, p.idTorneo, rondas, fecha) match
        case Fallo(motivo) => Resumen(Nil, motivo, Nil, cabecera)
        case Exito(tablaPuntos) =>
          val ordenados = tablaPuntos.equipos.sortBy(r => -r.puntosTotales)
          val maximo = ordenados.headOption.map(r => r.puntosTotales)
          val campeones = ordenados.filter(r => Some(r.puntosTotales) == maximo).map(r => r.equipo)
          val filas = ordenados.zipWithIndex.map((r, i) =>
            List(s"${i + 1}.º", r.equipo.nombreEquipo, s"${r.puntosTotales} puntos"))
          Resumen(campeones, s"${maximo.getOrElse(0)} puntos en ${rondas.length} ronda(s)", filas,
            cabecera + s"\nRondas jugadas: ${rondas.length}")
  }

  private def resumenEliminacion(t: Torneo, cabecera: String): Resumen = {
    val registros = c.partidas.getOrElse(t.idTorneo, Nil)
    if registros.isEmpty then
      Resumen(Nil, "No se registraron partidas: no hay resultados para decidir un ganador", Nil, cabecera)
    else {
      val filas = registros.map(r =>
        List(s"Partida ${r.partida.idPartida}",
          s"${r.partida.equipo1.nombreEquipo} vs ${r.partida.equipo2.nombreEquipo}",
          r.partida.resumen() + r.ganador.map(g => s"  —  ganó ${g.nombreEquipo}").getOrElse("  —  empate")))
      // La última partida con ganador se toma como la final.
      val campeon = registros.reverse.flatMap(r => r.ganador).headOption.toList
      Resumen(campeon, "Ganó la partida final", filas,
        cabecera + s"\nPartidas jugadas: ${registros.length}\nLa última partida registrada se considera la final.")
    }
  }

  private def resumir(t: Torneo): Resumen = {
    val cabecera =
      s"Torneo: ${t.nombreTorneo}\nTipo: ${Sistema.tipoDeTorneo(t)}\nVideojuego: ${t.videojuego.titulo}\n" +
      s"Modalidad: ${t.modalidad.descripcion()}\nOrganizador: ${Sistema.nombreOrganizador(c.estado, t.idTorneo)}\n" +
      s"Equipos participantes: ${t.equipos.length}"
    t match
      case p: TorneoSistemaDePuntos => resumenPuntos(p, cabecera)
      case _                        => resumenEliminacion(t, cabecera)
  }

  private def mostrar(): Unit =
    Ui.seleccion(cbTorneo) match
      case None =>
        lblInfo.setText("Todavía no hay torneos finalizados. El organizador debe finalizar uno desde la pestaña Torneos.")
        tarjeta.mostrar("SIN RESULTADOS", "—", "Aquí aparecerá el ganador cuando termine un torneo")
        Ui.llenar(modelo, Nil)
        txtResumen.setText("")
      case Some(t) =>
        val r = resumir(t)
        lblInfo.setText(s"Torneo finalizado  |  Modalidad ${t.modalidad.descripcion()}: ${t.modalidad.requisito()}")
        r.campeones match
          case Nil =>
            tarjeta.mostrar("SIN GANADOR DEFINIDO", "—", r.detalle)
          case unico :: Nil =>
            val integrantes = unico.integrantes.map(i => i.nickname).mkString(", ")
            tarjeta.mostrar("★  CAMPEÓN  ★", unico.nombreEquipo, s"Integrantes: $integrantes   ·   ${r.detalle}")
          case varios =>
            tarjeta.mostrar("★  CAMPEONES (EMPATE)  ★", varios.map(e => e.nombreEquipo).mkString(" / "), r.detalle)
        Ui.llenar(modelo, r.filas)
        txtResumen.setText(r.texto)
        txtResumen.setCaretPosition(0)

  // Se llama al cambiar el estado y cada vez que se abre la pestaña (las rondas y partidas
  // se registran sin notificar al resto de pantallas).
  def refrescar(): Unit = {
    val terminados = c.estado.torneos.filter(t => terminado(t))
    Ui.rellenar(cbTorneo, terminados.map(t =>
      Opcion(t, s"#${t.idTorneo} ${t.nombreTorneo} [${Sistema.tipoDeTorneo(t)}]", t.idTorneo)))
    mostrar()
  }

  Ui.margen(this)

  private val arriba = new JPanel(new BorderLayout())
  arriba.add(Ui.formulario(List(("Torneo finalizado:", cbTorneo))), BorderLayout.CENTER)
  arriba.add(lblInfo, BorderLayout.SOUTH)
  Ui.conTitulo(arriba, "Resumen de un torneo finalizado")

  private val clasificacion = new JPanel(new BorderLayout())
  clasificacion.add(Ui.scroll(tabla), BorderLayout.CENTER)
  Ui.conTitulo(clasificacion, "Clasificación / desarrollo del torneo")

  private val datos = new JPanel(new BorderLayout())
  datos.add(Ui.desplazable(txtResumen, 300, 200), BorderLayout.CENTER)
  Ui.conTitulo(datos, "Datos del torneo")

  private val division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clasificacion, datos)
  division.setResizeWeight(0.62)
  division.setBorder(null)
  division.setDividerSize(8)

  private val centro = new JPanel(new BorderLayout(10, 10))
  centro.add(tarjeta, BorderLayout.NORTH)
  centro.add(division, BorderLayout.CENTER)

  add(arriba, BorderLayout.NORTH)
  add(centro, BorderLayout.CENTER)

  cbTorneo.addActionListener(_ => mostrar())
  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
// Tarjetas de estadísticas (se muestran arriba al iniciar sesión)
// ---------------------------------------------------------------------
class PanelEstadisticas(c: Controlador) extends JPanel(new GridLayout(1, 4, 14, 0)) {
  private val tTorneos = new TarjetaDato("Torneos", Tema.Cian)
  private val tActivos = new TarjetaDato("Torneos activos", Tema.Verde)
  private val tJuegos = new TarjetaDato("Videojuegos", Tema.Violeta)
  private val tParticipantes = new TarjetaDato("Participantes", Tema.Magenta)

  setBorder(BorderFactory.createEmptyBorder(14, 14, 0, 14))
  List(tTorneos, tActivos, tJuegos, tParticipantes).foreach(t => add(t))

  private def refrescar(): Unit = {
    val e = c.estado
    tTorneos.valor(e.torneos.length)
    tActivos.valor(Sistema.torneosActivos(e).length)
    tJuegos.valor(e.videojuegos.length)
    tParticipantes.valor(Sistema.participantes(e).length)
  }

  c.suscribir(() => refrescar())
}

// ---------------------------------------------------------------------
// Ventana principal
// ---------------------------------------------------------------------
class VentanaPrincipal extends JFrame("VEXORA — Gestor de Torneos de Videojuegos") {
  private val control = new Controlador

  private val lblEstado = Ui.nombrar(new JLabel(" "), "lblEstado")

  private def mostrarMensaje(texto: String, esError: Boolean): Unit = {
    lblEstado.setForeground(if esError then Tema.Rojo else Tema.Verde)
    lblEstado.setText("●  " + texto)
  }

  // Cuentas de ejemplo: 1 organizador y 6 participantes (los mismos de Main.scala).
  private def cargarEjemplo(): Unit = {
    val juego = Videojuego(1, "Call Of Duty", "FPS")

    val participantes = List(
      Participante(1, "Tokkf", 1, "Diego", "a@gmail.com"),
      Participante(2, "La Trucha", 2, "Matias", "b@gmail.com"),
      Participante(3, "Leon", 3, "Paolo", "c@gmail.com"),
      Participante(4, "Marco", 4, "Marco", "d@gmail.com"),
      Participante(5, "Ana", 5, "Ana", "e@gmail.com"),
      Participante(6, "Beto", 6, "Beto", "f@gmail.com"))
    val organizador = Organizador(Nil, 7, "Organizador Demo", "organizador@gmail.com")
    val usuarios: List[Usuario] = organizador :: participantes

    val equipos = List(
      Equipo(1, "Triple t", List(participantes(0))),
      Equipo(2, "Enanos Verdes", List(participantes(1))),
      Equipo(3, "Gatitos feudales", List(participantes(2))),
      Equipo(4, "Los Cuatro", List(participantes(3))),
      Equipo(5, "Dúo", List(participantes(4), participantes(5))))

    val torneoPuntos = TorneoSistemaDePuntos(10, 2, 1, "Copa Warzone", juego, Individual, Nil, TorneoPendiente)
    val torneoDirecta = TorneoEliminacionDirecta(2, "Torneo Relámpago", juego, Individual, Nil, TorneoPendiente)

    val inscripciones = List(1, 2).flatMap(idTorneo => List(1, 2, 3, 4).map(idEquipo => (idTorneo, idEquipo)))

    val operacion: EstadoSistema => Resultado[EstadoSistema] = inicio => {
      val conUsuarios = usuarios.foldLeft(Exito[EstadoSistema](inicio): Resultado[EstadoSistema])((acc, u) =>
        acc.encadenar(e => Sistema.registrarUsuario(e, u)))
      val conEquipos = equipos.foldLeft(conUsuarios)((acc, equipo) =>
        acc.encadenar(e => Sistema.registrarEquipo(e, equipo)))
      val preparado = conEquipos
        .encadenar(e => Sistema.registrarVideojuegoComo(e, organizador.id, juego))
        .encadenar(e => Sistema.crearTorneo(e, organizador.id, torneoPuntos))
        .encadenar(e => Sistema.crearTorneo(e, organizador.id, torneoDirecta))
      inscripciones.foldLeft(preparado)((acc, par) =>
        acc.encadenar(e => Sistema.inscribirEquipo(e, par._1, par._2)))
    }

    control.ejecutar(operacion, "Datos de ejemplo cargados: 1 organizador, 6 participantes, 1 videojuego, 5 equipos y 2 torneos")
  }

  // Los paneles se crean una sola vez; al iniciar sesión se muestran solo los del rol.
  private val panelVideojuegos = new PanelVideojuegos(control)
  private val panelTorneosOrganizador = new PanelTorneos(control, true)
  private val panelInscripciones = new PanelInscripciones(control)
  private val panelPuntuacion = new PanelPuntuacion(control)
  private val panelPartidas = new PanelPartidas(control)
  private val panelTorneosConsulta = new PanelTorneos(control, false)
  private val panelUnirse = new PanelUnirse(control)
  private val panelMisInscripciones = new PanelMisInscripciones(control)
  private val panelEstadisticas = new PanelEstadisticas(control)
  private val panelResumen = new PanelResumenTorneo(control)

  private val pestanas = new JTabbedPane()
  private val tarjetas = new CardLayout
  private val contenido = new JPanel(tarjetas)

  private val lblSesion = Ui.nombrar(new JLabel(" "), "lblSesion")
  private val btnSalir = Ui.nombrar(Ui.boton("Cerrar sesión", true)(salir()), "btnSalir")
  private val guia = new JLabel("Inicia sesión o crea una cuenta para comenzar")

  private def descripcionRol(usuario: Usuario): String = usuario match
    case _: Organizador  => "Organizador"
    case p: Participante => s"Participante (${p.nickname})"
    case _               => "Usuario"

  private def entrar(usuario: Usuario): Unit = {
    control.iniciarSesion(usuario.id)
    pestanas.removeAll()
    usuario match
      case _: Organizador =>
        pestanas.addTab("Videojuegos", panelVideojuegos)
        pestanas.addTab("Torneos", panelTorneosOrganizador)
        pestanas.addTab("Inscripciones", panelInscripciones)
        pestanas.addTab("Puntuación", panelPuntuacion)
        pestanas.addTab("Partidas", panelPartidas)
        pestanas.addTab("Resumen de torneo", panelResumen)
      case _: Participante =>
        pestanas.addTab("Torneos", panelTorneosConsulta)
        pestanas.addTab("Unirme a un torneo", panelUnirse)
        pestanas.addTab("Mis inscripciones", panelMisInscripciones)
        pestanas.addTab("Resumen de torneo", panelResumen)
      case _ => ()
    lblSesion.setText(descripcionRol(usuario))
    lblSesion.setVisible(true)
    btnSalir.setVisible(true)
    guia.setVisible(false)
    tarjetas.show(contenido, "app")
    control.notificar()
    control.avisar(s"Sesión iniciada: ${descripcionRol(usuario)}")
  }

  private def salir(): Unit = {
    control.cerrarSesion()
    pestanas.removeAll()
    lblSesion.setVisible(false)
    btnSalir.setVisible(false)
    guia.setVisible(true)
    tarjetas.show(contenido, "acceso")
    control.avisar("Sesión cerrada")
  }

  private val panelAcceso = new PanelAcceso(control, usuario => entrar(usuario), () => cargarEjemplo())

  // ----- Estilo de la ventana -----
  pestanas.setUI(new PestanasVex)
  pestanas.setOpaque(true)
  pestanas.setBackground(Tema.Panel)
  pestanas.setFont(Tema.fuente(Font.BOLD, 13f))
  // Las rondas y partidas se registran sin avisar al resto de pantallas: se relee el resumen al abrirlo.
  pestanas.addChangeListener(_ => if pestanas.getSelectedComponent eq panelResumen then panelResumen.refrescar())

  private val panelApp = new JPanel(new BorderLayout())
  panelApp.add(panelEstadisticas, BorderLayout.NORTH)
  panelApp.add(pestanas, BorderLayout.CENTER)

  contenido.add(panelAcceso, "acceso")
  contenido.add(panelApp, "app")
  tarjetas.show(contenido, "acceso")

  // Cabecera: logotipo a la izquierda, sesión a la derecha.
  private val cabecera = new Cabecera
  cabecera.add(new LogoVexora(false), BorderLayout.WEST)
  lblSesion.setForeground(Tema.Texto)
  lblSesion.setFont(Tema.fuente(Font.BOLD, 14f))
  guia.setForeground(Tema.TextoSuave)
  private val derechaCabecera = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12))
  derechaCabecera.setOpaque(false)
  derechaCabecera.add(guia)
  derechaCabecera.add(lblSesion)
  derechaCabecera.add(btnSalir)
  lblSesion.setVisible(false)
  btnSalir.setVisible(false)
  cabecera.add(derechaCabecera, BorderLayout.EAST)

  // Barra inferior con los mensajes de éxito / error.
  private val barraEstado = new JPanel(new BorderLayout())
  barraEstado.setBackground(Tema.Fondo)
  barraEstado.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.Borde),
    BorderFactory.createEmptyBorder(7, 16, 7, 16)))
  private val lblMarca = new JLabel("VEXORA · Gestor de Torneos")
  lblMarca.setForeground(Tema.TextoSuave)
  lblMarca.setFont(Tema.fuente(Font.PLAIN, 12f))
  barraEstado.add(lblEstado, BorderLayout.WEST)
  barraEstado.add(lblMarca, BorderLayout.EAST)

  control.ventana = this
  control.alMensaje = (texto, esError) => mostrarMensaje(texto, esError)

  getContentPane.setLayout(new BorderLayout())
  getContentPane.setBackground(Tema.Panel)
  getContentPane.add(cabecera, BorderLayout.NORTH)
  getContentPane.add(contenido, BorderLayout.CENTER)
  getContentPane.add(barraEstado, BorderLayout.SOUTH)

  setIconImage(LogoVexora.icono)
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setMinimumSize(new Dimension(1000, 700))
  setSize(1200, 800)
  setLocationRelativeTo(null)
}

// ---------------------------------------------------------------------
// Punto de entrada
// ---------------------------------------------------------------------
object InterfazGrafica {
  def main(args: Array[String]): Unit = {
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")
    SwingUtilities.invokeLater(() => {
      Tema.instalar()
      val ventana = new VentanaPrincipal
      ventana.setVisible(true)
    })
  }
}
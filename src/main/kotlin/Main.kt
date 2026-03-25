import com.formdev.flatlaf.themes.FlatMacDarkLaf
import java.awt.Color
import java.awt.Font
import java.awt.Image
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import kotlin.math.hypot
import kotlin.math.sqrt


fun ImageIcon.scaled(width: Int, height: Int): ImageIcon = ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH))
/**
 * Application entry point
 */
fun main() {
    FlatMacDarkLaf.setup()          // Initialise the LAF

    val app = App()                 // Get an app state object
    val window = MainWindow(app)    // Spawn the UI, passing in the app state

    SwingUtilities.invokeLater { window.show() }
}


/**
 * Manage app state
 *
 * @property name the user's name
 * @property score the points earned
 */
class App {
    var name = "Test"
    var score = 0
    var locations = listOf(
        Location("Nut Den", mutableListOf(1,2), ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 82, 233, 420, 505),
        Location("Old Tree Shack", mutableListOf(0, 3), ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 111, 287, 75, 247),
        Location("Tree House", mutableListOf(0, 3, 4), ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 321, 496, 205, 396),
        Location("Pine Tower", mutableListOf(1, 2, 4), ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 578, 693, 74, 272),
        Location("Hole Home", mutableListOf(2, 3, 5), ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 746, 921, 335, 463),
        Location("Cave Manor", mutableListOf(4), ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 871, 1023, 74, 184)
    )

    var currentLocation = locations[0]
    var selectLocation = locations[0]
    var travelling = false
    var travelProgress:Double = 0.00

    fun scorePoints(points: Int) {
        score += points
    }

    fun resetScore() {
        score = 0
    }

    fun maxScoreReached(): Boolean {
        return score >= 5000
    }
}


/**
 * Main UI window, handles user clicks, etc.
 *
 * @param app the app state object
 */
class MainWindow(val app: App) {
    val frame = JFrame("Nutdealer")
    private val panel = JPanel().apply { layout = null }

    private val gameBackgroundLabel = JLabel()
    private val backgroundImage = ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524)
    private val UIbackgroundLabel = JLabel()
    private val UIBackgroundImage = ImageIcon(ClassLoader.getSystemResource("images/UserInterfaceBackground.png")).scaled(1080, 202)
    private val locationMarker = JLabel()
    private val markerImage = ImageIcon(ClassLoader.getSystemResource("images/Marker.png")).scaled(70, 70)
    private val dealerLocation = JLabel()
    private val dealerIcon = ImageIcon(ClassLoader.getSystemResource("images/NutDealerIcon.png")).scaled(70, 70)

    private var locationName = JLabel("Nut Den")
    private var travelButton = JButton("Travel to")
    private var travelPopup = JLabel("Travelling...")
    private val travelTimer = Timer(10, null)

    init {
        setupLayout()
        setupStyles()
        setupActions()
        setupWindow()
        updateUI()
    }

    private fun setupLayout() {
        panel.preferredSize = java.awt.Dimension(1080, 726)

        gameBackgroundLabel.setBounds(0, 0, 1080, 524)
        UIbackgroundLabel.setBounds(0, 524, 1080, 202)
        locationName.setBounds(890, 550, 300, 30)
        travelButton.setBounds(890, 590, 100, 30)
        travelPopup.setBounds(890, 590, 100, 30)

        locationMarker.setBounds((app.selectLocation.coordXMin+app.selectLocation.coordXMax)/2-35, app.selectLocation.coordYMin-70, 70, 70)
        dealerLocation.setBounds((app.currentLocation.coordXMin+app.currentLocation.coordXMax)/2-35, app.currentLocation.coordYMax-70, 70, 70)

        panel.add(dealerLocation)
        panel.add(locationMarker)
        panel.add(locationName)
        panel.add(travelButton)
        panel.add(travelPopup)
        panel.add(gameBackgroundLabel)
        panel.add(UIbackgroundLabel)
    }

    private fun setupStyles() {
        gameBackgroundLabel.icon = backgroundImage
        UIbackgroundLabel.icon = UIBackgroundImage
        locationMarker.icon = markerImage
        dealerLocation.icon = dealerIcon

        locationName.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        locationName.foreground = Color.BLACK

        travelPopup.font = Font(Font.SANS_SERIF, Font.BOLD, 13)
        travelPopup.foreground = Color.BLACK
    }

    private fun setupWindow() {
        frame.isResizable = false                           // Can't resize
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE  // Exit upon window close
        frame.contentPane = panel                           // Define the main content
        frame.pack()
        frame.setLocationRelativeTo(null)                   // Centre on the screen
    }

    private fun setupActions() {
//      Handles selecting location by finding if location of click was in the area of a location
        gameBackgroundLabel.addMouseListener( handleLocationClick())

        travelButton.addActionListener{ handleTravelClick() }
        travelTimer.addActionListener{ handleTravelTween() }
    }

    private fun handleLocationClick(): MouseListener {
        return object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                for (location in app.locations){
                    if(e.x in location.coordXMin..location.coordXMax && e.y in location.coordYMin..location.coordYMax && !app.travelling){
                        app.selectLocation = location
                        updateUI()
                    }
                }
            }
        }
    }

    private fun handleTravelClick() {
        app.travelling = true
        app.travelProgress = 0.00
        travelTimer.start()
        updateUI()
    }

    private fun handleTravelTween() {
        val initialX = (app.currentLocation.coordXMin+app.currentLocation.coordXMax)/2-35
        val initialY = app.currentLocation.coordYMax-70
        val endX = (app.selectLocation.coordXMin+app.selectLocation.coordXMax)/2-35
        val endY = app.selectLocation.coordYMax-70

        dealerLocation.setBounds((initialX + (endX - initialX) * app.travelProgress).toInt(), (initialY + (endY - initialY) * app.travelProgress).toInt(), 70, 70 )


        //Idk fix
        app.travelProgress += hypot((initialX + (endX - initialX).toDouble()), (initialY + (endY - initialY).toDouble()))/30000

        if (app.travelProgress >= 1.00){
            travelTimer.stop()
            app.travelling = false
            app.currentLocation = app.selectLocation
            updateUI()
        }
    }

    fun updateUI() {
        locationName.text = app.selectLocation.name
        locationMarker.setBounds((app.selectLocation.coordXMin+app.selectLocation.coordXMax)/2-35, app.selectLocation.coordYMin-70, 70, 70)
        dealerLocation.setBounds((app.currentLocation.coordXMin+app.currentLocation.coordXMax)/2-35, app.currentLocation.coordYMax-70, 70, 70)

        if (app.locations.indexOf(app.selectLocation) in app.currentLocation.adjacentIndex){ travelButton.isVisible = true}
        else { travelButton.isVisible = false }

        if (app.travelling) {
            travelPopup.isVisible = true
            travelButton.isVisible = false
        }
        else {travelPopup.isVisible = false}

    }

    fun show() {
        frame.isVisible = true
    }
}

class Location(val name: String, val adjacentIndex: MutableList<Int>, val backgroundImage: ImageIcon, val coordXMin: Int, val coordXMax: Int, val coordYMin: Int, val coordYMax: Int) {

}
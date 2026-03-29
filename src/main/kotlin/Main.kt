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
    var locations = listOf( // Creates each location with required arguments
        Location("Nut Den", mutableListOf(1,2), ImageIcon(ClassLoader.getSystemResource("images/NutDen.png")).scaled(1080, 524), 82, 233, 420, 505),
        Location("Old Tree Shack", mutableListOf(0, 3), ImageIcon(ClassLoader.getSystemResource("images/TreeShack.png")).scaled(1080, 524), 111, 287, 75, 247),
        Location("Tree House", mutableListOf(0, 3, 4), ImageIcon(ClassLoader.getSystemResource("images/TreeHouse.png")).scaled(1080, 524), 321, 496, 205, 396),
        Location("Pine Tower", mutableListOf(1, 2, 4), ImageIcon(ClassLoader.getSystemResource("images/PineTower.png")).scaled(1080, 524), 578, 693, 74, 272),
        Location("Hole Home", mutableListOf(2, 3, 5), ImageIcon(ClassLoader.getSystemResource("images/HoleHome.png")).scaled(1080, 524), 746, 921, 335, 463),
        Location("Cave Manor", mutableListOf(4), ImageIcon(ClassLoader.getSystemResource("images/CaveManor.png")).scaled(1080, 524), 871, 1023, 74, 184)
    )
    var isOnWorldMap = true


    var currentLocation = locations[0]
    var selectLocation = locations[0]
    var travelling = false
    var travelProgress:Double = 0.00

    fun harvestAcorn(){

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
    private val MapImage = ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524)
    private val UIbackgroundLabel = JLabel()
    private val UIBackgroundImage = ImageIcon(ClassLoader.getSystemResource("images/UserInterfaceBackground.png")).scaled(1080, 202)
    private val locationMarker = JLabel()
    private val markerImage = ImageIcon(ClassLoader.getSystemResource("images/Marker.png")).scaled(70, 70)
    private val dealerLocation = JLabel()
    private val dealerIcon = ImageIcon(ClassLoader.getSystemResource("images/NutDealerIcon.png")).scaled(70, 70)

    private val pots = mutableListOf<JButton>( JButton("1"), JButton("2"), JButton("3"), JButton("4"))

    private val potEmptyImage = ImageIcon(ClassLoader.getSystemResource("images/potEmpty.png")).scaled(130, 285)
    private val potWateredImage = ImageIcon(ClassLoader.getSystemResource("images/potWatered.png")).scaled(130, 285)
    private val potGrownImage = ImageIcon(ClassLoader.getSystemResource("images/potGrown.png")).scaled(130, 285)
    private val potReadyImage = ImageIcon(ClassLoader.getSystemResource("images/potReady.png")).scaled(130, 285)

    private var locationName = JLabel("Nut Den")
    private var travelButton = JButton("Travel to")
    private var travelPopup = JLabel("Travelling...")
    private val travelTimer = Timer(10, null)

    private var toggleLocationButton = JButton("Enter ${app.currentLocation.name}")

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
        toggleLocationButton.setBounds(890, 590, 180, 30)

        locationMarker.setBounds((app.selectLocation.coordXMin+app.selectLocation.coordXMax)/2-35, app.selectLocation.coordYMin-70, 70, 70)
        dealerLocation.setBounds((app.currentLocation.coordXMin+app.currentLocation.coordXMax)/2-35, app.currentLocation.coordYMax-70, 70, 70)

        pots[0].setBounds(80, 140, 130, 285)
        pots[1].setBounds(210, 220, 130, 285)
        pots[2].setBounds(340, 100, 130, 285)
        pots[3].setBounds(470, 190, 130, 285)

        for (pot in pots){
            panel.add(pot)
        }

        panel.add(dealerLocation)
        panel.add(locationMarker)
        panel.add(locationName)
        panel.add(travelButton)
        panel.add(toggleLocationButton)
        panel.add(travelPopup)
        panel.add(gameBackgroundLabel)
        panel.add(UIbackgroundLabel)
    }

    private fun setupStyles() {
        gameBackgroundLabel.icon = MapImage
        UIbackgroundLabel.icon = UIBackgroundImage
        locationMarker.icon = markerImage
        dealerLocation.icon = dealerIcon

        for (pot in pots){
            pot.icon = potEmptyImage
            pot.isBorderPainted = false
            pot.isContentAreaFilled = false
            pot.isFocusPainted = false

        }

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
        gameBackgroundLabel.addMouseListener( handleMouseClick())

        travelButton.addActionListener{ handleTravelClick() }
        toggleLocationButton.addActionListener{ handleLocationClick() }

        travelTimer.addActionListener{ handleTravelTween() }

        for (i in pots.indices){
            pots[i].addActionListener{ handlePotClick(i) }
        }
    }

    //  ---------------------------------- MOUSE INPUT HANDLERS
    private fun handleMouseClick(): MouseListener {
        return object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (app.isOnWorldMap){ // Blocks checking when in a location
                    for (location in app.locations){ // Checks if click location is in bounding box of a location
                        if(e.x in location.coordXMin..location.coordXMax && e.y in location.coordYMin..location.coordYMax && !app.travelling){
                            app.selectLocation = location
                            updateUI()
                        }
                    }
                }
            }
        }
    }

    //  ---------------------------------- BUTTON INPUT HANDLERS
    private fun handleTravelClick() {
        app.travelling = true
        app.travelProgress = 0.00
        travelTimer.start()
        updateUI()
    }

    private fun handleLocationClick(){
        app.isOnWorldMap = !app.isOnWorldMap
        updateUI()
    }

    private fun handlePotClick(potID: Int){
        pots[potID].icon = potWateredImage
        val acorntimer = Timer(1000, null)
        acorntimer.addActionListener{ growAcorn(potID) }
        acorntimer.start()
    }

    //  ---------------------------------- TIMER HANDLERS
    private fun handleTravelTween() {
        val initialX = (app.currentLocation.coordXMin+app.currentLocation.coordXMax)/2-35
        val initialY = app.currentLocation.coordYMax-70
        val endX = (app.selectLocation.coordXMin+app.selectLocation.coordXMax)/2-35
        val endY = app.selectLocation.coordYMax-70

        // Handles
        val dx = (endX - initialX).toDouble()
        val dy = (endY - initialY).toDouble()
        val dist = hypot(dx, dy)
        val stepSize = 2
        val journeySteps = dist / stepSize

        app.travelProgress++

        if (app.travelProgress >= journeySteps) {
            travelTimer.stop()
            app.travelling = false
            app.currentLocation = app.selectLocation
            updateUI()
        } else {
            val newX = initialX + (dx / journeySteps) * app.travelProgress
            val newY = initialY + (dy / journeySteps) * app.travelProgress

            dealerLocation.setBounds(newX.toInt(), newY.toInt(), 70, 70 )
        }
    }

    private fun growAcorn( potID: Int ){
        if (pots[potID].icon == potGrownImage || pots[potID].icon == potReadyImage){
            pots[potID].icon = potReadyImage
        } else { pots[potID].icon = potGrownImage }
    }

    //  ---------------------------------- UPDATE FUNCTION
    fun updateUI() {
        if (app.isOnWorldMap) {
            gameBackgroundLabel.icon = MapImage
            locationName.text = app.selectLocation.name
            toggleLocationButton.text = "Enter ${app.currentLocation.name}"
            locationMarker.setBounds((app.selectLocation.coordXMin + app.selectLocation.coordXMax) / 2 - 35, app.selectLocation.coordYMin - 70, 70, 70)
            dealerLocation.setBounds((app.currentLocation.coordXMin + app.currentLocation.coordXMax) / 2 - 35, app.currentLocation.coordYMax - 70, 70, 70)

            dealerLocation.isVisible = true
            locationMarker.isVisible = true
            travelButton.isVisible = app.locations.indexOf(app.selectLocation) in app.currentLocation.adjacentIndex

            for (pot in pots){
                pot.isVisible = false
            }

            if (app.travelling) {
                travelPopup.isVisible = true
                travelButton.isVisible = false
            } else { travelPopup.isVisible = false }

            if (app.currentLocation == app.selectLocation) { toggleLocationButton.isVisible = true }
            else { toggleLocationButton.isVisible = false }
        } else {
            dealerLocation.isVisible = false
            locationMarker.isVisible = false
            toggleLocationButton.text = "Exit ${app.currentLocation.name}"

            gameBackgroundLabel.icon = app.currentLocation.backgroundImage

            when (app.currentLocation){
                 app.locations[0] -> {
                     for (pot in pots){
                         pot.isVisible = true
                     }
                 }
            }
        }
    }

    fun show() {
        frame.isVisible = true
    }
}

class Location(val name: String, val adjacentIndex: MutableList<Int>, val backgroundImage: ImageIcon, val coordXMin: Int, val coordXMax: Int, val coordYMin: Int, val coordYMax: Int) {

}
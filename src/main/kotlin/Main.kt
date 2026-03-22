import com.formdev.flatlaf.themes.FlatMacDarkLaf
import java.awt.Color
import java.awt.Font
import java.awt.Image
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*


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
        Location("Nut Den", ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 82, 233, 420, 505),
        Location("Old Tree Shack", ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 111, 287, 75, 247),
        Location("Tree House", ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 321, 496, 205, 396),
        Location("Pine Tower", ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 578, 693, 74, 272),
        Location("Hole Home", ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 746, 921, 335, 463),
        Location("Cave Manor", ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524), 871, 1023, 74, 184)
    )


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

    private val infoWindow = InfoWindow(this, app)      // Pass app state to dialog too

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

        panel.add(gameBackgroundLabel)
        panel.add(UIbackgroundLabel)


    }

    private fun setupStyles() {
        gameBackgroundLabel.icon = backgroundImage
        UIbackgroundLabel.icon = UIBackgroundImage

    }

    private fun setupWindow() {
        frame.isResizable = false                           // Can't resize
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE  // Exit upon window close
        frame.contentPane = panel                           // Define the main content
        frame.pack()
        frame.setLocationRelativeTo(null)                   // Centre on the screen
    }

    private fun setupActions() {
        gameBackgroundLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                // This code runs when the label is clicked
                println("${e.x}, ${e.y}")

                for (location in app.locations){
                    if(e.x in location.coordXMin..location.coordXMax && e.y in location.coordYMin..location.coordYMax ){
                        println(location.name)
                    }

                }
            }
        })

    }

    private fun handleMainClick() {
        app.scorePoints(1000)       // Update the app state
        updateUI()                  // Update this window UI to reflect this
    }

    private fun handleInfoClick() {
        infoWindow.show()
    }

    fun updateUI() {


        infoWindow.updateUI()       // Keep child dialog window UI up-to-date too
    }

    fun show() {
        frame.isVisible = true
    }
}


/**
 * Info UI window is a child dialog and shows how the
 * app state can be shown / updated from multiple places
 *
 * @param owner the parent frame, used to position and layer the dialog correctly
 * @param app the app state object
 */
class InfoWindow(val owner: MainWindow, val app: App) {
    private val dialog = JDialog(owner.frame, "DIALOG TITLE", false)
    private val panel = JPanel().apply { layout = null }

    private val infoLabel = JLabel()
    private val resetButton = JButton("Reset")

    init {
        setupLayout()
        setupStyles()
        setupActions()
        setupWindow()
        updateUI()
    }

    private fun setupLayout() {
        panel.preferredSize = java.awt.Dimension(240, 180)

        infoLabel.setBounds(30, 30, 180, 60)
        resetButton.setBounds(30, 120, 180, 30)

        panel.add(infoLabel)
        panel.add(resetButton)
    }

    private fun setupStyles() {
        infoLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 16)
        resetButton.font = Font(Font.SANS_SERIF, Font.PLAIN, 16)
    }

    private fun setupWindow() {
        dialog.isResizable = false                              // Can't resize
        dialog.defaultCloseOperation = JDialog.HIDE_ON_CLOSE    // Hide upon window close
        dialog.contentPane = panel                              // Main content panel
        dialog.pack()
    }

    private fun setupActions() {
        resetButton.addActionListener { handleResetClick() }
    }

    private fun handleResetClick() {
        app.resetScore()    // Update the app state
        owner.updateUI()    // Update the UI to reflect this, via the main window
    }

    fun updateUI() {
        // Use app properties to display state
        infoLabel.text = "<html>User: ${app.name}<br>Score: ${app.score} points"

        resetButton.isEnabled = app.score > 0
    }

    fun show() {
        val ownerBounds = owner.frame.bounds          // get location of the main window
        dialog.setLocation(                           // Position next to main window
            ownerBounds.x + ownerBounds.width + 10,
            ownerBounds.y
        )

        dialog.isVisible = true
    }
}

class Location(val name: String, val backgroundImage: ImageIcon, val coordXMin: Int, val coordXMax: Int, val coordYMin: Int, val coordYMax: Int) {

}
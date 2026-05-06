import com.formdev.flatlaf.themes.FlatMacDarkLaf
import java.awt.Color
import java.awt.Font
import java.awt.Image
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.swing.*
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.system.exitProcess

//CONSTANTS
const val MAP_ICON_SIZE = 70
const val MAX_ORDERS = 3
const val NOTORIETY_MIN_X = 331
const val NOTORIETY_MAX_X = 831
const val ACORN_VALUE = 100
const val NOTORIETY_PASSIVE_DECREASE = 0.0012 //0.0012 default
const val STEP_SIZE = 2

//Scaling for images
fun ImageIcon.scaled(width: Int, height: Int): ImageIcon = ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH))

/**
 * Application entry point
 */
fun main() {
    FlatMacDarkLaf.setup()          // Initialise the LAF

    val game = Game()                 // Get an app state object
    val window = MainWindow(game)    // Spawn the UI, passing in the app state
    SwingUtilities.invokeLater { window.show() }
}

enum class GameState{ //Creates an Enumeration that can be used to define game states for both the UI updates and gameplay
    MENU, WORLD, LOCATION, ENDGAME, TUTORIAL
}

enum class DragType{ //Drag type enum for different action with the drag/drop instances
    NONE, SEED, WATER
}

fun playSound(bytes: ByteArray): Clip {
    // The sound bytes are passed to an audio stream thread
    val stream = AudioSystem.getAudioInputStream(bytes.inputStream())
    val sound = AudioSystem.getClip().apply {
        open(stream)
        start()
        addLineListener { if (it.type == LineEvent.Type.STOP) close() }
    }
    return sound
}

fun stopSound(currentClip:Clip) {
    currentClip.stop()
    currentClip.close()
}

/**
 * Manage app state
 * Game Class handles the actual gameplay, variables and functions
 */
class Game {
    var locations = listOf( // Creates each location with required arguments
        Location("Nut Den", mutableListOf(1,2), ImageIcon(ClassLoader.getSystemResource("images/NutDen.png")).scaled(1080, 524), 0.0,82, 233, 420, 505),
        Location("Old Tree Shack", mutableListOf(0, 3), ImageIcon(ClassLoader.getSystemResource("images/TreeShack.png")).scaled(1080, 524), 0.4, 111, 287, 75, 247),
        Location("Tree House", mutableListOf(0, 3, 4), ImageIcon(ClassLoader.getSystemResource("images/TreeHouse.png")).scaled(1080, 524), 0.5, 321, 496, 205, 396),
        Location("Pine Tower", mutableListOf(1, 2, 4), ImageIcon(ClassLoader.getSystemResource("images/PineTower.png")).scaled(1080, 524), 0.7, 578, 693, 74, 272),
        Location("Hole Home", mutableListOf(2, 3, 5), ImageIcon(ClassLoader.getSystemResource("images/HoleHome.png")).scaled(1080, 524), 1.0, 746, 921, 335, 463),
        Location("Cave Manor", mutableListOf(4), ImageIcon(ClassLoader.getSystemResource("images/CaveManor.png")).scaled(1080, 524), 1.20, 871, 1023, 74, 184)
    )

    //Create and set game variables
    var gamestate = GameState.MENU
    var pageIndex = 0

    var currentLocation = locations[0]
    var selectLocation = locations[0]
    var travelling = false
    var travelProgress:Double = 0.00

    private var orderLikelihood = 0.00

    var notoriety = 0.50
    var acorns = 0
    var cash = 0
    var highscore = 0
    private var globalDifficultyMultiplier = 1.00

    var instancedDragObject = JLabel()
    var dragtype = DragType.NONE

    var orders = mutableListOf<Order>()

    /**
    * Function to add an acorn to the players inventory
    * */
    fun harvestAcorn(){ //Adds an acorn
        acorns++
    }

    /**
    * When order timer is called, creates a random chance to create an order
    * gets valid locations and chooses best one based on difficulty to add an order do and then creates an order
    * */
    fun handleOrderTimer() { //Function for creating orders
        //Difficulty scaled random order chance
        val randomOrderChance = Random.nextDouble(0.0,3.0)
        if ((randomOrderChance+(globalDifficultyMultiplier*1.2))+orderLikelihood > (2.75+globalDifficultyMultiplier)){
            //Gets valid locations to create an order at
            val possibleLocations = locations.filter { it.currentOrder == null && it.name != "Nut Den" }
            orderLikelihood = 0.00
            if (orders.size < MAX_ORDERS) {
                //Weighted random location chooser, higher the difficulty more likely to create at further away locations (higher difficultyWeight)
                val randomSeed = Random.nextDouble(0.2, 1.0)
                val weightedSeed = (randomSeed * globalDifficultyMultiplier)
                val location = possibleLocations.minByOrNull { abs(it.difficultyWeight - weightedSeed) }

                //Safely creates on order at that location if there isn't one
                location?.let { orders.add(it.createOrder(globalDifficultyMultiplier)) }
            }
        }else if(orders.size < MAX_ORDERS) orderLikelihood += 0.015
    }

    /**
    * Removes acorns, increases score, increases difficulty scaling, and removes the order
    * */
    fun completeOrder(){
        acorns -= currentLocation.currentOrder?.acornNum ?: 0
        cash += currentLocation.currentOrder?.acornNum?.times(ACORN_VALUE) ?: 0
        globalDifficultyMultiplier += Random.nextDouble(0.05, 0.15)
        playSound(ClassLoader.getSystemResourceAsStream("sounds/squirrel.wav")!!.readBytes())
        playSound(ClassLoader.getSystemResourceAsStream("sounds/winOrder.wav")!!.readBytes())
        removeOrder(currentLocation, false)
    }

    /**
    * Adds or removes notoriety based on order success and then removes the order
    * */
    fun removeOrder(location: Location, isFail: Boolean){
        val notorietyAmount = location.currentOrder?.notorietyAmount?.toDouble() ?: 0.0

        if (isFail) notoriety -= (notorietyAmount / 100)
        else notoriety += (notorietyAmount / 100)

        orders.remove(location.currentOrder)
        location.removeOrder()
    }

    /**
    * Restarts game, clearing orders, and resetting game data
    * */
    fun reset(){
        for (location in locations){
            orders.remove(location.currentOrder)
            location.removeOrder()
        }
        notoriety = 0.5
        acorns = 0
        cash = 0
        currentLocation = locations[0]
        selectLocation = locations[0]
        globalDifficultyMultiplier = 1.00
    }
}

/**
 * Main UI window, handles user clicks, updating and any graphical elements.
 *
 * @param game the app state object
 */
class MainWindow(val game: Game) {
    private val frame = JFrame("Nutdealer")
    private val panel = JLayeredPane().apply { layout = null }

    //Constant Elements
    private val gameBackgroundLabel = JLabel()
    private val mapImage = ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524)
    private val backgroundUILabel = JLabel()
    private val backgroundUIImage = ImageIcon(ClassLoader.getSystemResource("images/UserInterfaceBackground.png")).scaled(1080, 202)
    private var fullscreenImage = ImageIcon()
    private val fullscreenLabel = JButton()
    private val tutorialPages = listOf(
        ImageIcon(ClassLoader.getSystemResource("images/Tutorial1.png")).scaled(1080, 726),
        ImageIcon(ClassLoader.getSystemResource("images/Tutorial2.png")).scaled(1080, 726),
        ImageIcon(ClassLoader.getSystemResource("images/Tutorial3.png")).scaled(1080, 726)
    )
    private var referenceableSound = playSound(ClassLoader.getSystemResourceAsStream("sounds/menuMusic.wav")!!.readBytes())

    //World Map Elements
    private val locationMarker = JLabel()
    private val markerImage = ImageIcon(ClassLoader.getSystemResource("images/Marker.png")).scaled(MAP_ICON_SIZE, MAP_ICON_SIZE)
    private val dealerLocation = JLabel()
    private val dealerIcon = ImageIcon(ClassLoader.getSystemResource("images/NutDealerIcon.png")).scaled(MAP_ICON_SIZE, MAP_ICON_SIZE)

    //Location contextual Elements

    //Nut Den
    private val pots = mutableListOf(JLabel("1"), JLabel("2"), JLabel("3"), JLabel("4"))

    private val potImages = mutableListOf(
        ImageIcon(ClassLoader.getSystemResource("images/potEmpty.png")).scaled(130, 285),
        ImageIcon(ClassLoader.getSystemResource("images/potSeed.png")).scaled(130, 285),
        ImageIcon(ClassLoader.getSystemResource("images/potWatered.png")).scaled(130, 285),
        ImageIcon(ClassLoader.getSystemResource("images/potWatered.png")).scaled(130, 285),
        ImageIcon(ClassLoader.getSystemResource("images/potGrowing.png")).scaled(130, 285),
        ImageIcon(ClassLoader.getSystemResource("images/potGrown.png")).scaled(130, 285),
        ImageIcon(ClassLoader.getSystemResource("images/potReady.png")).scaled(130, 285)
    )

    private val seedSpawner = JLabel()
    private val seedImage = ImageIcon(ClassLoader.getSystemResource("images/SeedPack.png")).scaled(100, 100)
    private val waterSpawner = JLabel()
    private val waterImage = ImageIcon(ClassLoader.getSystemResource("images/WaterDrop.png")).scaled(100, 100)

    //Other Location
    private val customerElement = JLabel()
    private val customerSpeech = JLabel()
    private val speechImage = ImageIcon(ClassLoader.getSystemResource("images/SpeechBubble.png")).scaled(271, 176)
    private val speechText = JLabel()
    private val orderButton = JButton()
    private val orderButtonImage = ImageIcon(ClassLoader.getSystemResource("images/orderButton.png")).scaled(230, 40)


    //User Interface

    //Context Actions
    private var locationName = JLabel("Nut Den")
    private var travelButton = JButton("Travel to")
    private var travelPopup = JLabel("Travelling...")
    private var toggleLocationButton = JButton("Enter ${game.currentLocation.name}")

    //User Information
    private val nutAmount = JLabel(game.acorns.toString())
    private val cash = JLabel(game.cash.toString())

    private val notorietyMarker = JLabel()
    private val notorietyImage = ImageIcon(ClassLoader.getSystemResource("images/NoterietyMarker.png")).scaled(25, 25)

    private val orderLabels = mutableListOf<JLabel>()
    private val orderNames = mutableListOf<JLabel>()
    private val orderLocations = mutableListOf<JLabel>()
    private val orderNutAmount = mutableListOf<JLabel>()
    private val orderRepAmount = mutableListOf<JLabel>()
    private val orderTimerProgress = mutableListOf<JProgressBar>()
    private val orderImage = ImageIcon(ClassLoader.getSystemResource("images/OrderNotification.png")).scaled(277, 47)

    //Timers
    private val travelTimer = Timer(10, null)
    private val seedDragTimer = Timer(10, null)
    private val progressbarTimer = Timer(300, null)
    private val orderDirectorTimer = Timer(1000, null)

    init {
        //Creates the order notification objects, done in the init because logic can't be done in declarations
        for (i in 0 until MAX_ORDERS){
            orderLabels.add(JLabel())
            orderNames.add(JLabel())
            orderLocations.add(JLabel())
            orderNutAmount.add(JLabel())
            orderRepAmount.add(JLabel())
            orderTimerProgress.add(JProgressBar())
            orderTimerProgress[i].value = 100
        }
        //Other setup
        setupLayout()
        setupStyles()
        setupActions()
        setupWindow()
        updateUI()
    }

    private fun setupLayout() {
        //SETS BOUNDS AND ADDS ALL ELEMENTS TO PANEL
        panel.preferredSize = java.awt.Dimension(1080, 726)

        //Background Elements
        gameBackgroundLabel.setBounds(0, 0, 1080, 524)
        backgroundUILabel.setBounds(0, 524, 1080, 202)
        fullscreenLabel.setBounds(0,0,1080,726)

        //UI Elements
        locationName.setBounds(890, 550, 300, 30)
        travelButton.setBounds(890, 590, 100, 30)
        travelPopup.setBounds(890, 590, 100, 30)
        toggleLocationButton.setBounds(890, 590, 180, 30)
        nutAmount.setBounds(500, 640, 100, 100)
        cash.setBounds(770, 640, 100, 100)
        notorietyMarker.setBounds(831, 600, 25, 25)

        //Graphical Elements
        locationMarker.setBounds((game.selectLocation.coordXMin+game.selectLocation.coordXMax)-MAP_ICON_SIZE, game.selectLocation.coordYMin-MAP_ICON_SIZE, MAP_ICON_SIZE, MAP_ICON_SIZE)
        dealerLocation.setBounds((game.currentLocation.coordXMin+game.currentLocation.coordXMax)-MAP_ICON_SIZE, game.currentLocation.coordYMax-MAP_ICON_SIZE, MAP_ICON_SIZE, MAP_ICON_SIZE)
        customerElement.setBounds(500, 40, 484, 484)
        customerSpeech.setBounds(300, 20, 277, 176)
        speechText.setBounds(310, 30, 240, 100)
        orderButton.setBounds(310, 90, 230, 40)


        //Nut Den Elements
        seedSpawner.setBounds(430, 20, 100, 100)
        waterSpawner.setBounds(550, 20, 100, 100)

        //Has to individually set the pot locations
        pots[0].setBounds(80, 140, 130, 285)
        pots[1].setBounds(210, 220, 130, 285)
        pots[2].setBounds(340, 100, 130, 285)
        pots[3].setBounds(470, 190, 130, 285)

        //Order notification starting Y value
        var initialLabelY = 570
        var initialtextY = 563

        //Order stepping distance for creating list
        val yStep = 50

        for (i in 0 until MAX_ORDERS){//For all the created notification elements
            //Set the location and size
            orderLabels[i].setBounds( 33, initialLabelY, 277, 47)
            orderNames[i].setBounds( 36, initialtextY, 277, 47)
            orderLocations[i].setBounds( 118, initialtextY, 277, 47)
            orderNutAmount[i].setBounds(245, initialtextY, 277, 47)
            orderRepAmount[i].setBounds(285, initialtextY, 277, 47)
            orderTimerProgress[i].setBounds(40, initialtextY+37, 260, 10)

            //Add them to the panel
            panel.add(orderLabels[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderNames[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderLocations[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderNutAmount[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderRepAmount[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderTimerProgress[i], JLayeredPane.DEFAULT_LAYER)

            //Step the Y coords for the next set
            initialLabelY += yStep
            initialtextY += yStep
        }

        for (pot in pots){ //Adds pots
            panel.add(pot, JLayeredPane.DEFAULT_LAYER)
        }

        // Add all elements to screen
        panel.add(notorietyMarker, JLayeredPane.DEFAULT_LAYER)
        panel.add(orderButton, JLayeredPane.DEFAULT_LAYER)
        panel.setLayer(orderButton, JLayeredPane.DEFAULT_LAYER+1)
        panel.add(speechText, JLayeredPane.DEFAULT_LAYER)
        panel.setLayer(speechText, JLayeredPane.DEFAULT_LAYER+1)
        panel.add(customerSpeech, JLayeredPane.DEFAULT_LAYER)
        panel.add(customerElement, JLayeredPane.DEFAULT_LAYER)
        panel.add(waterSpawner, JLayeredPane.DEFAULT_LAYER)
        panel.add(seedSpawner, JLayeredPane.DEFAULT_LAYER)
        panel.add(nutAmount, JLayeredPane.DEFAULT_LAYER)
        panel.add(cash, JLayeredPane.DEFAULT_LAYER)
        panel.add(dealerLocation, JLayeredPane.DEFAULT_LAYER)
        panel.add(locationMarker, JLayeredPane.DEFAULT_LAYER)
        panel.add(locationName, JLayeredPane.DEFAULT_LAYER)
        panel.add(travelButton, JLayeredPane.DEFAULT_LAYER)
        panel.add(toggleLocationButton, JLayeredPane.DEFAULT_LAYER)
        panel.add(travelPopup, JLayeredPane.DEFAULT_LAYER)

        //Background Images, all elements layer over this
        panel.add(fullscreenLabel, JLayeredPane.DEFAULT_LAYER+1)
        panel.add(gameBackgroundLabel,JLayeredPane.DEFAULT_LAYER-1)
        panel.add(backgroundUILabel, JLayeredPane.DEFAULT_LAYER-1)
    }

    private fun setupStyles() {
        //STYLING AND IMAGES
        //Sets labels to use image
        gameBackgroundLabel.icon = mapImage
        backgroundUILabel.icon = backgroundUIImage
        locationMarker.icon = markerImage
        dealerLocation.icon = dealerIcon
        customerSpeech.icon = speechImage
        notorietyMarker.icon = notorietyImage
        fullscreenLabel.setBorderPainted(false)
        fullscreenLabel.setContentAreaFilled(false)
        fullscreenLabel.setFocusPainted(false)

        //Styles order button
        orderButton.icon = orderButtonImage
        orderButton.setBorderPainted(false)
        orderButton.setContentAreaFilled(false)
        orderButton.setFocusPainted(false)
        orderButton.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        orderButton.foreground = Color.BLACK
        orderButton.verticalTextPosition = SwingConstants.CENTER
        orderButton.horizontalTextPosition = SwingConstants.CENTER

        //Sets all pot images to the empty state
        for (pot in pots){
            pot.icon = potImages[0]
        }

        //Sets order notification backgrounds
        for (label in orderLabels){
            label.icon = orderImage
        }

        //Other styling
        seedSpawner.icon = seedImage
        waterSpawner.icon = waterImage

        locationName.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        locationName.foreground = Color.BLACK

        nutAmount.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        nutAmount.foreground = Color.BLACK

        cash.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        cash.foreground = Color.BLACK

        travelPopup.font = Font(Font.SANS_SERIF, Font.BOLD, 13)
        travelPopup.foreground = Color.BLACK

        speechText.font = Font(Font.SANS_SERIF, Font.BOLD, 15)
        speechText.foreground = Color.BLACK
        speechText.verticalAlignment = SwingConstants.TOP

        travelButton.isFocusPainted = false
        travelButton.background = Color(165, 116, 95)
        travelButton.foreground = Color.BLACK
        toggleLocationButton.isFocusPainted = false
        toggleLocationButton.background = Color(165, 116, 95)
        toggleLocationButton.foreground = Color.BLACK

        //Text styling for order notifications
        for (i in 0 until MAX_ORDERS){
            orderNames[i].font = Font(Font.SANS_SERIF, Font.BOLD, 13)
            orderNames[i].foreground = Color.BLACK
            orderLocations[i].font = Font(Font.SANS_SERIF, Font.BOLD, 13)
            orderLocations[i].foreground = Color.BLACK
            orderNutAmount[i].font = Font(Font.SANS_SERIF, Font.BOLD, 13)
            orderNutAmount[i].foreground = Color.BLACK
            orderRepAmount[i].font = Font(Font.SANS_SERIF, Font.BOLD, 13)
            orderRepAmount[i].foreground = Color.BLACK
        }
    }

    private fun setupWindow() {
        frame.isResizable = false                           // Can't resize
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE  // Exit upon window close
        frame.contentPane = panel                           // Define the main content
        frame.pack()
        frame.setLocationRelativeTo(null)                   // Centre on the screen
    }

    private fun setupActions() {
        //Adds function calls to actions

        //Mouse listener can be used for more customised clicking than button ActionListeners
        gameBackgroundLabel.addMouseListener( handleBackgroundClick())

        //Buttons
        fullscreenLabel.addActionListener{ handleTutorialClick() }
        travelButton.addActionListener{ handleTravelClick() }
        toggleLocationButton.addActionListener{ handleLocationClick() }
        orderButton.addActionListener { handleOrderHandover() }

        //Timer callers
        travelTimer.addActionListener{ handleTravelTween(referenceableSound) }
        seedDragTimer.addActionListener{ handleDrag() }
        orderDirectorTimer.addActionListener { handleOrderTimer() }
        progressbarTimer.addActionListener { handleProgressbar() }

        for (i in pots.indices){ //Pot interactions
            pots[i].addMouseListener(handleMousePot(i))
        }
    }

    private fun setupGame(){
        //Starts global timers and sets constant game elements to be visible like the UI and backgrounds
        orderDirectorTimer.start()
        progressbarTimer.start()
        gameBackgroundLabel.isVisible = true
        backgroundUILabel.isVisible=true
        notorietyMarker.isVisible=true
        cash.isVisible=true
        nutAmount.isVisible=true
        locationName.isVisible=true
        for (pot in pots){
            pot.icon = potImages[0]
        }

        //resets the nut amount and cash elements
        nutAmount.setBounds(500, 640, 100, 100)
        cash.setBounds(770, 640, 100, 100)
        nutAmount.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        nutAmount.foreground = Color.BLACK
        cash.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        cash.foreground = Color.BLACK

        game.reset()
    }

    private fun endGame(endScreenIcon: ImageIcon){//Stops game timers and sends the player to the end screen
        orderDirectorTimer.stop()
        progressbarTimer.stop()
        game.gamestate = GameState.ENDGAME
        fullscreenImage = endScreenIcon
        saveHighscore(game) //Writes score to a file if it is a new highscore, otherwise retrieves the highscore
        updateUI()
    }

    //  ---------------------------------- MOUSE INPUT HANDLERS
    private fun handleBackgroundClick(): MouseListener {
        /**
        * Custom mouse input overrides
        * */
        return object : MouseAdapter() {
            /*
            * Mouse down function for handling the drag operation for growing
            * */
            override fun mousePressed(e: MouseEvent?) {
                if (game.gamestate == GameState.LOCATION && game.currentLocation == game.locations[0]){
                    if (seedSpawner.bounds.contains(panel.mousePosition)){
                        game.dragtype = DragType.SEED
                        playSound(ClassLoader.getSystemResourceAsStream("sounds/location.wav")!!.readBytes())
                        handleDraggableClick(seedImage, 430)
                    } else if (waterSpawner.bounds.contains(panel.mousePosition)){
                        game.dragtype = DragType.WATER
                        playSound(ClassLoader.getSystemResourceAsStream("sounds/location.wav")!!.readBytes())
                        handleDraggableClick(waterImage,550)
                    }
                }
            }

            /*
            * Mouse up function for handling the drag operation for growing
            * */
            override fun mouseReleased(e: MouseEvent) {
                if (panel.mousePosition != null) {
                    if (game.gamestate == GameState.LOCATION) {
                        if (game.dragtype != DragType.NONE) {
                            exitSeedDrag()
                            for (pot in pots) {
                                if (pot.bounds.contains(panel.mousePosition)) {
                                    handlePotClick(pots.indexOf(pot), game.dragtype)
                                }
                            }
                            game.dragtype = DragType.NONE
                        }
                    } else if (game.gamestate == GameState.WORLD) {
                        /*
                    * Mouse click on world map for location selection
                    * */
                        for (location in game.locations) { // Checks if click location is in bounding box of a location
                            if (e.x in location.coordXMin..location.coordXMax && e.y in location.coordYMin..location.coordYMax && !game.travelling) {
                                game.selectLocation = location
                                playSound(ClassLoader.getSystemResourceAsStream("sounds/location.wav")!!.readBytes())
                                updateUI()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleMousePot(i:Int): MouseListener {
        /*
        * Handles click on pots normally, without any special drag drop operation
        * */
        return object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                //When clicked normally, interacts with clicked on pot
                handlePotClick(i,DragType.NONE)
            }
        }
    }

    //  ---------------------------------- BUTTON INPUT HANDLERS
    private fun handleTravelClick() {
        //Starts the nutdealer travelling timer
        game.travelling = true
        game.travelProgress = 0.00
        referenceableSound = playSound(ClassLoader.getSystemResourceAsStream("sounds/travel.wav")!!.readBytes())

        travelTimer.start()
        updateUI()
    }

    private fun handleLocationClick(){
        when(game.gamestate){ //Toggles the gamestate between the world map and locations
            GameState.WORLD-> game.gamestate = GameState.LOCATION
            GameState.LOCATION-> game.gamestate = GameState.WORLD
            else -> {} //Other two game states aren't used here so can be ignored
        }
        playSound(ClassLoader.getSystemResourceAsStream("sounds/door.wav")!!.readBytes())
        updateUI()
    }

    private fun handleDraggableClick(image:ImageIcon, spawnX:Int){
        //Creates an instance of the draggable element and adds to the screen, starting the drag timer
        game.instancedDragObject.setBounds(spawnX, 20, 100, 100)
        panel.add(game.instancedDragObject, JLayeredPane.DEFAULT_LAYER+1)
        game.instancedDragObject.icon = image
        panel.revalidate()
        panel.repaint()
        seedDragTimer.start()

    }

    private fun handleDrag(){
        //Gets the mouse position
        val mouseLoc = panel.mousePosition
        if (panel.mousePosition != null) {//Set the instanced object to the mouse position if the mouse is on the screen
            game.instancedDragObject.setBounds(mouseLoc.x - seedImage.iconWidth / 2, mouseLoc.y - seedImage.iconWidth / 2, 100, 100)
        } else exitSeedDrag()
    }

    private fun exitSeedDrag(){
        //Stops the timer and removes the instance
        panel.remove(game.instancedDragObject)
        playSound(ClassLoader.getSystemResourceAsStream("sounds/location.wav")!!.readBytes())
        panel.revalidate()
        panel.repaint()
        seedDragTimer.stop()
    }

    private fun handleTutorialClick(){
        //Cycles through tutorial pages then returns to menu at the end
        if (game.gamestate == GameState.TUTORIAL){
            playSound(ClassLoader.getSystemResourceAsStream("sounds/button.wav")!!.readBytes())
            game.pageIndex+=1
            if (game.pageIndex < tutorialPages.size){
                updateUI()
            }else {
                game.gamestate = GameState.MENU
                game.pageIndex = 0
                updateUI()
            }
        }
    }

    private fun handlePotClick(potID: Int, type: DragType){
        /*
        * Handles logic for pot interaction for drag and drops and clicking using an enum for the different interactions on the pot
        * */
        when(type){
            DragType.SEED->{//Sets pot state to the planted state
                if (pots[potID].icon == potImages[0]){
                    pots[potID].icon = potImages[1]
                    playSound(ClassLoader.getSystemResourceAsStream("sounds/seed.wav")!!.readBytes())
                }
            }
            DragType.WATER->{//Sets pot to the watered state if a seed has been planted
                if (pots[potID].icon == potImages[1]) {
                    pots[potID].icon = potImages[2]
                    playSound(ClassLoader.getSystemResourceAsStream("sounds/water.wav")!!.readBytes())
                    val acornTimer = Timer(2000, null)//Creates and starts a growing timer
                    if (!acornTimer.isRunning){
                        pots[potID].icon = potImages[3]
                        acornTimer.addActionListener{ potInteract(potID, acornTimer) }
                        acornTimer.restart()
                    }
                }
            }
            DragType.NONE ->{ //If normal click then harvest if acorn is grown
                if (pots[potID].icon == potImages.last()){
                    pots[potID].icon = potImages[0]
                    game.harvestAcorn()
                    playSound(ClassLoader.getSystemResourceAsStream("sounds/acorn.wav")!!.readBytes())
                    updateUI()
                }
            }
        }
    }

    private fun handleOrderHandover(){
        game.currentLocation.currentOrder?.acornNum?.let { //Safely runs if the locations order is valid
            if (game.acorns >= it){ //If the players acorns are greater than the order then complete
                game.completeOrder()
                updateUI()
            }
        }
    }

    private fun handleMenu(){//Returns to the main menu
        game.gamestate = GameState.MENU
        updateUI()
    }

    private fun handlePlayGame(){//Resets the game and starts it
        game.gamestate = GameState.WORLD
        setupGame()
        updateUI()
    }

    //  ---------------------------------- TIMER HANDLERS
    private fun handleTravelTween(sound: Clip) {
        //Start and end locations for travelling
        val initialX = (game.currentLocation.coordXMin+game.currentLocation.coordXMax)/2-(MAP_ICON_SIZE/2)
        val initialY = game.currentLocation.coordYMax-MAP_ICON_SIZE
        val endX = (game.selectLocation.coordXMin+game.selectLocation.coordXMax)/2-(MAP_ICON_SIZE/2)
        val endY = game.selectLocation.coordYMax-MAP_ICON_SIZE

        // Handles
        val dx = (endX - initialX).toDouble()
        val dy = (endY - initialY).toDouble()
        val dist = hypot(dx, dy)
        val journeySteps = dist / STEP_SIZE

        game.travelProgress++

        if (game.travelProgress >= journeySteps) {//Ends the travelling on reaching destination
            travelTimer.stop()
            game.travelling = false
            game.currentLocation = game.selectLocation
            if (game.gamestate == GameState.WORLD) stopSound(sound)
            updateUI()
        } else {//Travels a step toward destination
            val newX = initialX + (dx / journeySteps) * game.travelProgress
            val newY = initialY + (dy / journeySteps) * game.travelProgress

            dealerLocation.setBounds(newX.toInt(), newY.toInt(), MAP_ICON_SIZE, MAP_ICON_SIZE )
        }
    }

    private fun potInteract( potID: Int , timer: Timer){
        //Growing Logic, increasing the growth stage until it is fully grown.
        val currentIndex = potImages.indexOf(pots[potID].icon)
        pots[potID].icon = potImages[currentIndex+1]
        if (pots[potID].icon == potImages.last()){
            timer.stop()
        }
    }

    private fun handleOrderTimer(){
        game.handleOrderTimer()//Attempts random chance order creation
        updateUI()
    }

    private fun handleProgressbar(){
        /*
        * Function for handling progress bar for both notoriety and order timers.
        *
        * Reduces notoriety, checks if game should end
        * and also updates and checks each orders timer and progress bar element
        * */

        game.notoriety-= NOTORIETY_PASSIVE_DECREASE//Reduces Notoriety slightly over time

        if (game.notoriety <= 0.0){ //Ends game with homeless ending if notoriety = 0
            game.notoriety = 0.0
            notorietyMarker.setBounds(NOTORIETY_MIN_X, 600, 25, 25)
            stopSound(referenceableSound)
            endGame(ImageIcon(ClassLoader.getSystemResource("images/HomelessEnding.png")))
            referenceableSound= playSound(ClassLoader.getSystemResourceAsStream("sounds/homeless.wav")!!.readBytes())
        }else if (game.notoriety >= 1.0){ //Ends game with jail ending if notoriety = 1
            game.notoriety = 1.0
            notorietyMarker.setBounds(NOTORIETY_MAX_X, 600, 25, 25)
            stopSound(referenceableSound)
            endGame(ImageIcon(ClassLoader.getSystemResource("images/JailEnding.png")))
            referenceableSound= playSound(ClassLoader.getSystemResourceAsStream("sounds/jail.wav")!!.readBytes())
        } else notorietyMarker.setBounds((NOTORIETY_MIN_X + game.notoriety * (NOTORIETY_MAX_X - NOTORIETY_MIN_X)).toInt(), 600, 25, 25)

        for (location in game.locations){
            val orderIndex = game.orders.indexOf(location.currentOrder)//gets the order list index of the locations order
            if (orderIndex in game.orders.indices){
                location.currentOrder?.reduceOrderTimer()//reduces order time
                orderTimerProgress[orderIndex].value = location.currentOrder?.timerVal ?: 100 //Updates the progress bar
                if(orderTimerProgress[orderIndex].value <= 0){ //Removes order when timer reaches 0
                    orderTimerProgress[orderIndex].value = 100
                    playSound(ClassLoader.getSystemResourceAsStream("sounds/failOrder.wav")!!.readBytes())
                    game.removeOrder(location,true)
                    handleProgressbar()
                    updateUI()
                }
            }
        }
    }

    //  ---------------------------------- UPDATE FUNCTIONS
    /**
    * Checks Game State and calls update function for specific State, and updates order visibility and data
    * */
    fun updateUI() {
        when(game.gamestate){ //Separate updates for each gamestate
            GameState.MENU-> menuUpdate()

            GameState.WORLD-> worldUpdate()

            GameState.LOCATION-> locationUpdate()

            GameState.ENDGAME-> endGameUpdate()

            GameState.TUTORIAL->tutorialUpdate()
        }

        //Order notification updates
        for (i in 0 until MAX_ORDERS){ //Shows the notifications for each order
            val visible = i < game.orders.size
            orderLabels[i].isVisible = visible
            orderNames[i].isVisible = visible
            orderLocations[i].isVisible = visible
            orderNutAmount[i].isVisible = visible
            orderRepAmount[i].isVisible = visible
            orderTimerProgress[i].isVisible = visible
        }

        for (i in game.orders.indices){//Updates order information
            orderNames[i].text = game.orders[i].customerName
            orderLocations[i].text = game.orders[i].locationName
            orderNutAmount[i].text = game.orders[i].acornNum.toString()
            orderRepAmount[i].text = game.orders[i].notorietyAmount.toString()
        }
    }

    /*
    * Main menu update function, hides every component, makes the fullscreen label visible and creates the play button
    * */
    private fun menuUpdate(){
        fullscreenLabel.isVisible = true
        fullscreenLabel.icon = ImageIcon(ClassLoader.getSystemResource("images/MenuImage.png")).scaled(1080, 724)

        panel.components.forEach {//Hides all other elements
            component ->  if(component != fullscreenLabel)  component.isVisible = false
        }

        val playButton = JButton() //Creates a play button
        val tutorialButton = JButton()
        val quitButton = JButton()

        playButton.setBounds(25, 600, 200, 87)
        tutorialButton.setBounds(250, 600, 200, 87)
        quitButton.setBounds(474, 600, 200, 87)

        panel.add(playButton)
        panel.add(tutorialButton)
        panel.add(quitButton)
        panel.setLayer(playButton, JLayeredPane.DEFAULT_LAYER+2)
        panel.setLayer(tutorialButton, JLayeredPane.DEFAULT_LAYER+2)
        panel.setLayer(quitButton, JLayeredPane.DEFAULT_LAYER+2)

        playButton.icon = ImageIcon(ClassLoader.getSystemResource("images/PlayButton.png")).scaled(200, 87)
        playButton.isBorderPainted = false
        playButton.setContentAreaFilled(false)
        playButton.setFocusPainted(false)
        tutorialButton.icon = ImageIcon(ClassLoader.getSystemResource("images/TutorialButton.png")).scaled(200, 87)
        tutorialButton.isBorderPainted = false
        tutorialButton.setContentAreaFilled(false)
        tutorialButton.setFocusPainted(false)
        quitButton.icon = ImageIcon(ClassLoader.getSystemResource("images/QuitButton.png")).scaled(200, 87)
        quitButton.isBorderPainted = false
        quitButton.setContentAreaFilled(false)
        quitButton.setFocusPainted(false)

        playButton.addActionListener{ //Starts game and removes buttons
            playSound(ClassLoader.getSystemResourceAsStream("sounds/button.wav")!!.readBytes())
            stopSound(referenceableSound)
            panel.remove(playButton)
            panel.remove(tutorialButton)
            panel.remove(quitButton)
            handlePlayGame()
        }

        tutorialButton.addActionListener{ //Starts tutorial
            playSound(ClassLoader.getSystemResourceAsStream("sounds/button.wav")!!.readBytes())
            panel.remove(playButton)
            panel.remove(tutorialButton)
            panel.remove(quitButton)
            game.gamestate = GameState.TUTORIAL
            updateUI()
        }

        quitButton.addActionListener{
            playSound(ClassLoader.getSystemResourceAsStream("sounds/button.wav")!!.readBytes())
            exitProcess(0)
        }
    }

    /*
    * Update for world map gamestate, makes world and travelling elements visible and hides location elements
    * */
    private fun worldUpdate(){
        //World Map setup
        gameBackgroundLabel.icon = mapImage
        locationName.text = game.selectLocation.name
        toggleLocationButton.text = "Enter ${game.currentLocation.name}"
        fullscreenLabel.isVisible = false
        nutAmount.text = game.acorns.toString()
        cash.text = game.cash.toString()
        notorietyMarker.setBounds((NOTORIETY_MIN_X + game.notoriety * (NOTORIETY_MAX_X - NOTORIETY_MIN_X)).toInt(), 600, 25, 25)

        dealerLocation.isVisible = true
        locationMarker.isVisible = true
        travelButton.isVisible = game.locations.indexOf(game.selectLocation) in game.currentLocation.adjacentIndex

        //Hides location elements
        customerSpeech.isVisible = false
        speechText.isVisible = false
        orderButton.isVisible = false
        customerElement.isVisible = false
        seedSpawner.isVisible = false
        waterSpawner.isVisible = false

        for (pot in pots) pot.isVisible = false

        //Travelling check
        if (game.travelling) {
            travelPopup.isVisible = true
            travelButton.isVisible = false
        } else{
            travelPopup.isVisible = false
            locationMarker.setBounds((game.selectLocation.coordXMin+game.selectLocation.coordXMax)/2-(MAP_ICON_SIZE/2), game.selectLocation.coordYMin-MAP_ICON_SIZE, MAP_ICON_SIZE, MAP_ICON_SIZE)
            dealerLocation.setBounds((game.currentLocation.coordXMin+game.currentLocation.coordXMax)/2-(MAP_ICON_SIZE/2), game.currentLocation.coordYMax-MAP_ICON_SIZE, MAP_ICON_SIZE, MAP_ICON_SIZE)
        }

        //Entering location check
        if (game.currentLocation == game.selectLocation) toggleLocationButton.isVisible = true
        else toggleLocationButton.isVisible = false
    }

    /*
    * Update for location gamestate, makes location and order elements visible (if needed) and hides world map elements
    * */
    private fun locationUpdate(){
        //Update for locations
        dealerLocation.isVisible = false
        locationMarker.isVisible = false

        //Global UI
        toggleLocationButton.text = "Exit ${game.currentLocation.name}"
        gameBackgroundLabel.icon = game.currentLocation.backgroundImage
        fullscreenLabel.isVisible = false
        nutAmount.text = game.acorns.toString()
        cash.text = game.cash.toString()
        notorietyMarker.setBounds((NOTORIETY_MIN_X + game.notoriety * (NOTORIETY_MAX_X - NOTORIETY_MIN_X)).toInt(), 600, 25, 25)

        when (game.currentLocation){
            game.locations[0] -> { //Pots and growing tools in nut den
                for (pot in pots){
                    pot.isVisible = true
                }
                seedSpawner.isVisible = true
                waterSpawner.isVisible = true
            }
            else -> { //Elements for customers and orders
                speechText.isVisible = game.currentLocation.currentOrder != null
                orderButton.isVisible = game.currentLocation.currentOrder != null
                orderButton.text = game.currentLocation.currentOrder?.acornNum.toString()
                speechText.text = game.currentLocation.currentOrder?.customerSpeech
                customerSpeech.isVisible = game.currentLocation.currentOrder != null
                customerElement.isVisible = game.currentLocation.currentOrder != null
                customerElement.icon = game.currentLocation.currentOrder?.customerIcon
            }
        }
    }

    /*
    * Changes the UI to the end game, showing the player score and high score, and covering all gameplay elements
    * */
    private fun endGameUpdate(){
        //Reuses the cash and acorn num elements and creates return button
        panel.components.forEach {//Hides all other elements
                component ->  if(component != fullscreenLabel)  component.isVisible = false
        }

        cash.text = (game.cash.toString())
        nutAmount.text = game.highscore.toString()
        val returnButton = JButton("Return to menu")
        cash.setBounds(450,500, 300, 100)
        nutAmount.setBounds(450,595, 300, 100)
        returnButton.setBounds(850, 595, 200, 120)
        returnButton.icon = ImageIcon(ClassLoader.getSystemResource("images/ReturnButton.png")).scaled(200, 120)
        returnButton.isBorderPainted = false
        returnButton.setContentAreaFilled(false)
        returnButton.setFocusPainted(false)

        //Updates panel
        cash.isVisible = true
        nutAmount.isVisible=true
        panel.setLayer(cash, JLayeredPane.DEFAULT_LAYER+2)
        panel.setLayer(nutAmount, JLayeredPane.DEFAULT_LAYER+2)
        panel.add(returnButton)
        panel.setLayer(returnButton, JLayeredPane.DEFAULT_LAYER+2)

        fullscreenLabel.isVisible=true
        fullscreenLabel.icon=fullscreenImage.scaled(1080,726)
        cash.font = Font(Font.SANS_SERIF, Font.BOLD, 60)
        cash.foreground = Color.WHITE
        nutAmount.font = Font(Font.SANS_SERIF, Font.BOLD, 60)
        nutAmount.foreground = Color.WHITE

        returnButton.addActionListener{ //Returns to menu and removes button
            panel.remove(returnButton)
            stopSound(referenceableSound)
            playSound(ClassLoader.getSystemResourceAsStream("sounds/button.wav")!!.readBytes())
            referenceableSound = playSound(ClassLoader.getSystemResourceAsStream("sounds/menuMusic.wav")!!.readBytes())
            handleMenu()
        }

        panel.revalidate()
        panel.repaint()
    }

    private fun tutorialUpdate(){
        //Updates to current tutorial page
        fullscreenLabel.icon = tutorialPages[game.pageIndex]
    }

    fun show() {
        frame.isVisible = true //Shows window
    }
}

class Location(val name: String, val adjacentIndex: MutableList<Int>, val backgroundImage: ImageIcon, val difficultyWeight: Double, val coordXMin: Int, val coordXMax: Int, val coordYMin: Int, val coordYMax: Int) {
    /**
    * The location class handles order storing and control, and uses a lot of parameters for the locations
    *
    * Adjacent locations for travelling
    * Difficulty weight for location distances for orders
    * Bounding box values for clicking on locations
    * */

    var currentOrder: Order? = null //Orders are a nullable variable

    fun createOrder(difficultyMultiplier:Double): Order{ //Creates an order if there isn't one
        currentOrder = currentOrder ?: Order(difficultyMultiplier, name)
        playSound(ClassLoader.getSystemResourceAsStream("sounds/notification.wav")!!.readBytes())
        return currentOrder!!
    }

    fun removeOrder(){ //Resets order to null
        currentOrder = null
    }
}

class Order(private val difficultyMultiplier:Double, val locationName:String) {
    /**
    * Creates customer data, name images and text
    *
    * generates notoriety and acorns based on difficulty
    * */
    val customerIcon = getIcon()
    val customerName = getName()
    val customerSpeech = getSpeechPrompt()
    val acornNum = getAcornAmount()
    val notorietyAmount = getRepAmount()
    var timerVal = 100

    private fun getIcon():ImageIcon {//Gets a random customer image
        val customerImages = listOf(
            ImageIcon(ClassLoader.getSystemResource("images/Customer1.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer2.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer3.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer4.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer5.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer6.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer7.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer8.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer9.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer10.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer11.png")).scaled(484, 484),
            ImageIcon(ClassLoader.getSystemResource("images/Customer12.png")).scaled(484, 484)
        )
        return customerImages.random()
    }

    private fun getName():String {//Gets a random customer name
        val customerNames = listOf(
            "Emanuel",
            "Johnny",
            "David",
            "Frederick",
            "Floppy",
            "Arthur",
            "Lennayy",
            "Mr. Nuts",
            "Rusty",
            "Peanut",
            "Snickers",
            "Peter",
            "Hugh Jass",
            "Mike Oxlong",
            "Joel",
            "Johnette",
            "Johnabella",
            "Susan",
            "Alex",
            "Sir Scoop"
        )
        return customerNames.random()
    }

    private fun getSpeechPrompt(): String{//Get a random speech prompt
        val promptsList = listOf(//<html> tag for auto text wrapping
            "<html>You got the nuts? I have the cash.</html>",
            "<html>I uh... h-hello... Give them here.</html>",
            "<html>Hurry up I don't have time to deal with this.</html>",
            "<html>Broo... This is the good stuff yo.</html>",
            "<html>God you don't know how long I've been waiting.</html>",
            "<html>g-g-g-give them i need them i need them i need them.</html>",
            "<html>Why thank you my good sir!</html>",
            "<html>Alright here's the cash.</html>",
            "<html>Don't try anything alright?</html>"
        )
        return promptsList.random()
    }

    private fun getAcornAmount():Int{ //Gets a random acorn amount scaling with difficulty
        val acorns = (Random.nextDouble(1.0, 2.0) * (difficultyMultiplier*difficultyMultiplier)).toInt()
        return acorns
    }

    private fun getRepAmount():Int{ //Gets a random reputation amount scaling with difficulty
        val notoriety = (Random.nextInt(10, 30) * (difficultyMultiplier)).toInt()
        return notoriety
    }

    fun reduceOrderTimer(){//Reduces timer
        timerVal -= 1
    }
}

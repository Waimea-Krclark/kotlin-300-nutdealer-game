import com.formdev.flatlaf.themes.FlatMacDarkLaf
import java.awt.Color
import java.awt.Font
import java.awt.Image
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random


//CONSANTS
const val MAP_ICON_SIZE = 70
const val MAX_ORDERS = 3 //Used for testing keep on 3 otherwise won't work with current graphical assets
const val NOTORIETY_MIN_X = 331
const val NOTORIETY_MAX_X = 831
const val ACORN_VALUE = 100


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


/**
 * Manage app state
 *
 *
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
    var isOnWorldMap = true

    var currentLocation = locations[0]
    var selectLocation = locations[0]
    var travelling = false
    var travelProgress:Double = 0.00

    var orderLikelihood = 0.00

    var notoriety = 0.50
    var acorns = 0
    var cash = 0
    var globalDifficultyMultiplier = 1.00

    var instancedDragObject = JLabel()
    var dragtype = ""

    var orders = mutableListOf<Order>()

    fun harvestAcorn(){
        acorns++
    }

    fun handleOrderTimer() {
        val randomOrderChance = Random.nextDouble(0.0,3.0)
        if ((randomOrderChance*globalDifficultyMultiplier)+orderLikelihood > 2.9){
            val possibleLocations = locations.filter { it.currentOrder == null && it.name != "Nut Den" }
            orderLikelihood = 0.00
            if (orders.size < 3) {
                val randomSeed = Random.nextDouble(0.2, 1.0)
                val weightedSeed = (randomSeed * globalDifficultyMultiplier)

                val location = possibleLocations.minByOrNull { abs(it.difficultyWeight - weightedSeed) }

                location?.let { orders.add(it.createOrder(globalDifficultyMultiplier)) }
            }
        }else orderLikelihood += 0.05
    }

    fun completeOrder(){
        acorns -= currentLocation.currentOrder?.acornNum ?: 0
        cash += currentLocation.currentOrder?.acornNum?.times(ACORN_VALUE) ?: 0
        globalDifficultyMultiplier += Random.nextDouble(0.05, 0.15)
        removeOrder(currentLocation, false)
    }

    fun removeOrder(location: Location, isFail: Boolean){
        val notorietyAmount = location.currentOrder?.notorietyAmount?.toDouble() ?: 0.0

        if (isFail) notoriety -= (notorietyAmount / 100)
        else notoriety += (notorietyAmount / 100)

        orders.remove(location.currentOrder)
        location.currentOrder = null
    }
}


/**
 * Main UI window, handles user clicks, etc.
 *
 * @param app the app state object
 */
class MainWindow(val game: Game) {
    val frame = JFrame("Nutdealer")
    private val panel = JLayeredPane().apply { layout = null }

    //Constant Elements
    private val gameBackgroundLabel = JLabel()
    private val mapImage = ImageIcon(ClassLoader.getSystemResource("images/Map.png")).scaled(1080, 524)
    private val UIbackgroundLabel = JLabel()
    private val UIBackgroundImage = ImageIcon(ClassLoader.getSystemResource("images/UserInterfaceBackground.png")).scaled(1080, 202)

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
    private val notorietyImage = ImageIcon(ClassLoader.getSystemResource("images/noterietyMarker.png")).scaled(25, 25)

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
        orderDirectorTimer.start()
        progressbarTimer.start()
        for (i in 0 until MAX_ORDERS){
            orderLabels.add(JLabel())
            orderNames.add(JLabel())
            orderLocations.add(JLabel())
            orderNutAmount.add(JLabel())
            orderRepAmount.add(JLabel())
            orderTimerProgress.add(JProgressBar())
            orderTimerProgress[i].value = 100
        }
        setupLayout()
        setupStyles()
        setupActions()
        setupWindow()
        updateUI()
    }

    private fun setupLayout() {
        panel.preferredSize = java.awt.Dimension(1080, 726)

        //Background Elements
        gameBackgroundLabel.setBounds(0, 0, 1080, 524)
        UIbackgroundLabel.setBounds(0, 524, 1080, 202)

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

        pots[0].setBounds(80, 140, 130, 285)
        pots[1].setBounds(210, 220, 130, 285)
        pots[2].setBounds(340, 100, 130, 285)
        pots[3].setBounds(470, 190, 130, 285)

        var initialLabelY = 570
        var initialtextY = 563

        val yStep = 50

        for (i in 0 until MAX_ORDERS){
            orderLabels[i].setBounds( 33, initialLabelY, 277, 47)
            orderNames[i].setBounds( 36, initialtextY, 277, 47)
            orderLocations[i].setBounds( 118, initialtextY, 277, 47)
            orderNutAmount[i].setBounds(245, initialtextY, 277, 47)
            orderRepAmount[i].setBounds(285, initialtextY, 277, 47)
            orderTimerProgress[i].setBounds(40, initialtextY+37, 260, 10)

            panel.add(orderLabels[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderNames[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderLocations[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderNutAmount[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderRepAmount[i], JLayeredPane.DEFAULT_LAYER)
            panel.add(orderTimerProgress[i], JLayeredPane.DEFAULT_LAYER)

            initialLabelY += yStep
            initialtextY += yStep
        }

        for (pot in pots){
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
        panel.add(gameBackgroundLabel,JLayeredPane.DEFAULT_LAYER-1)
        panel.add(UIbackgroundLabel, JLayeredPane.DEFAULT_LAYER-1)
    }

    private fun setupStyles() {
        gameBackgroundLabel.icon = mapImage
        UIbackgroundLabel.icon = UIBackgroundImage
        locationMarker.icon = markerImage
        dealerLocation.icon = dealerIcon
        customerSpeech.icon = speechImage
        notorietyMarker.icon = notorietyImage

        orderButton.icon = orderButtonImage
        orderButton.setBorderPainted(false)
        orderButton.setContentAreaFilled(false)
        orderButton.setFocusPainted(false)
        orderButton.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
        orderButton.foreground = Color.BLACK
        orderButton.verticalTextPosition = SwingConstants.CENTER
        orderButton.horizontalTextPosition = SwingConstants.CENTER

        for (pot in pots){
            pot.icon = potImages[0]
        }

        for (label in orderLabels){
            label.icon = orderImage
        }

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
        gameBackgroundLabel.addMouseListener( handleBackgroundClick())

        travelButton.addActionListener{ handleTravelClick() }
        toggleLocationButton.addActionListener{ handleLocationClick() }
        orderButton.addActionListener { handleOrderHandover() }

        travelTimer.addActionListener{ handleTravelTween() }
        seedDragTimer.addActionListener{ handleDrag() }
        orderDirectorTimer.addActionListener { handleOrderTimer() }
        progressbarTimer.addActionListener { handleProgressbar() }

        for (i in pots.indices){
            pots[i].addMouseListener(handleMousePot(i))
        }
    }

    //  ---------------------------------- MOUSE INPUT HANDLERS
    private fun handleBackgroundClick(): MouseListener {
        return object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (game.isOnWorldMap){ // Blocks checking when in a location
                    for (location in game.locations){ // Checks if click location is in bounding box of a location
                        if(e.x in location.coordXMin..location.coordXMax && e.y in location.coordYMin..location.coordYMax && !game.travelling){
                            game.selectLocation = location
                            updateUI()
                        }
                    }
                }
            }

            override fun mousePressed(e: MouseEvent?) {
                if (!game.isOnWorldMap && game.currentLocation == game.locations[0]){
                    if (seedSpawner.bounds.contains(panel.mousePosition)){
                        game.dragtype = "seed"
                        handleDraggableClick(seedImage, 430)
                    } else if (waterSpawner.bounds.contains(panel.mousePosition)){
                        game.dragtype = "water"
                        handleDraggableClick(waterImage,550)
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent?) {
                if (!game.isOnWorldMap){
                    if (game.dragtype != ""){
                        exitSeedDrag()
                        for (pot in pots){
                            if (pot.bounds.contains(panel.mousePosition)){
                                handlePotClick(pots.indexOf(pot), game.dragtype)
                            }
                        }
                        game.dragtype = ""
                    }
                }
            }
        }
    }

    private fun handleMousePot(i:Int): MouseListener {
        return object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                handlePotClick(i,"mouse")
            }
        }
    }

    //  ---------------------------------- BUTTON INPUT HANDLERS
    private fun handleTravelClick() {
        game.travelling = true
        game.travelProgress = 0.00
        travelTimer.start()
        updateUI()
    }

    private fun handleLocationClick(){
        game.isOnWorldMap = !game.isOnWorldMap
        updateUI()
    }

    private fun handleDraggableClick(image:ImageIcon, spawnX:Int){
        game.instancedDragObject.setBounds(spawnX, 20, 100, 100)
        panel.add(game.instancedDragObject, JLayeredPane.DEFAULT_LAYER+1)
        game.instancedDragObject.icon = image
        panel.revalidate()
        panel.repaint()
        seedDragTimer.start()

    }

    private fun handleDrag(){
        val mouseLoc = panel.mousePosition
        try {
            game.instancedDragObject.setBounds(mouseLoc.x - seedImage.iconWidth / 2, mouseLoc.y - seedImage.iconWidth / 2, 100, 100)
        } catch (e:NullPointerException){
            exitSeedDrag()
        }
    }

    private fun exitSeedDrag(){
        panel.remove(game.instancedDragObject)
        panel.revalidate()
        panel.repaint()
        seedDragTimer.stop()
    }

    private fun handlePotClick(potID: Int,type:String){
        when(type){
            "seed"->{
                if (pots[potID].icon == potImages[0]){
                    pots[potID].icon = potImages[1]
                }
            }
            "water"->{
                if (pots[potID].icon == potImages[1]) {
                    pots[potID].icon = potImages[2]
                    val acorntimer = Timer(3000, null)
                    if (!acorntimer.isRunning){
                        pots[potID].icon = potImages[3]
                        acorntimer.addActionListener{ potInteract(potID, acorntimer) }
                        acorntimer.restart()
                    }
                }
            }
            else ->{
                if (pots[potID].icon == potImages.last()){
                    pots[potID].icon = potImages[0]
                    game.harvestAcorn()
                    updateUI()
                }
            }
        }
    }

    private fun handleOrderHandover(){
        game.currentLocation.currentOrder?.acornNum?.let {
            if (game.acorns >= it){
                game.completeOrder()
                updateUI()
            }
        }
    }

    //  ---------------------------------- TIMER HANDLERS
    private fun handleTravelTween() {
        val initialX = (game.currentLocation.coordXMin+game.currentLocation.coordXMax)/2-(MAP_ICON_SIZE/2)
        val initialY = game.currentLocation.coordYMax-MAP_ICON_SIZE
        val endX = (game.selectLocation.coordXMin+game.selectLocation.coordXMax)/2-(MAP_ICON_SIZE/2)
        val endY = game.selectLocation.coordYMax-MAP_ICON_SIZE

        // Handles
        val dx = (endX - initialX).toDouble()
        val dy = (endY - initialY).toDouble()
        val dist = hypot(dx, dy)
        val stepSize = 2
        val journeySteps = dist / stepSize

        game.travelProgress++

        if (game.travelProgress >= journeySteps) {
            travelTimer.stop()
            game.travelling = false
            game.currentLocation = game.selectLocation
            updateUI()
        } else {
            val newX = initialX + (dx / journeySteps) * game.travelProgress
            val newY = initialY + (dy / journeySteps) * game.travelProgress

            dealerLocation.setBounds(newX.toInt(), newY.toInt(), MAP_ICON_SIZE, MAP_ICON_SIZE )
        }
    }

    private fun potInteract( potID: Int , timer: Timer){
        val currentIndex = potImages.indexOf(pots[potID].icon)
        pots[potID].icon = potImages[currentIndex+1]
        if (pots[potID].icon == potImages.last()){
            timer.stop()
        }
    }

    private fun handleOrderTimer(){
        game.handleOrderTimer()
        updateUI()
    }

    private fun handleProgressbar(){
        game.notoriety-= 0.0012
        notorietyMarker.setBounds((NOTORIETY_MIN_X + game.notoriety * (NOTORIETY_MAX_X - NOTORIETY_MIN_X)).toInt(), 600, 25, 25)
        if (game.notoriety <= 0){

        }

        for (location in game.locations){
            val orderIndex = game.orders.indexOf(location.currentOrder)
            if (orderIndex in game.orders.indices){

                location.currentOrder?.reduceOrderTimer()
                orderTimerProgress[orderIndex].value = location.currentOrder?.timerVal ?: 100
                if(orderTimerProgress[orderIndex].value <= 0){
                    orderTimerProgress[orderIndex].value = 100
                    game.removeOrder(location,true)
                    handleProgressbar()
                    updateUI()
                }
            }
        }
    }

    //  ---------------------------------- UPDATE FUNCTION
    fun updateUI() {
        if (game.isOnWorldMap) {
            //World Map setup
            gameBackgroundLabel.icon = mapImage
            locationName.text = game.selectLocation.name
            toggleLocationButton.text = "Enter ${game.currentLocation.name}"

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

        } else { //Update for locations
            dealerLocation.isVisible = false
            locationMarker.isVisible = false

            toggleLocationButton.text = "Exit ${game.currentLocation.name}"
            gameBackgroundLabel.icon = game.currentLocation.backgroundImage

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

        //Global UI
        nutAmount.text = game.acorns.toString()
        cash.text = game.cash.toString()
        notorietyMarker.setBounds((NOTORIETY_MIN_X + game.notoriety * (NOTORIETY_MAX_X - NOTORIETY_MIN_X)).toInt(), 600, 25, 25)

        for (i in 0 until MAX_ORDERS){
            val visible = i < game.orders.size
            orderLabels[i].isVisible = visible
            orderNames[i].isVisible = visible
            orderLocations[i].isVisible = visible
            orderNutAmount[i].isVisible = visible
            orderRepAmount[i].isVisible = visible
            orderTimerProgress[i].isVisible = visible
        }

        for (i in game.orders.indices){
            orderNames[i].text = game.orders[i].customerName
            orderLocations[i].text = game.orders[i].locationName
            orderNutAmount[i].text = game.orders[i].acornNum.toString()
            orderRepAmount[i].text = game.orders[i].notorietyAmount.toString()
        }
    }

    fun show() {
        frame.isVisible = true
    }
}

class Location(val name: String, val adjacentIndex: MutableList<Int>, val backgroundImage: ImageIcon, val difficultyWeight: Double, val coordXMin: Int, val coordXMax: Int, val coordYMin: Int, val coordYMax: Int) {
    var currentOrder: Order? = null

    fun createOrder(difficultyMultiplier:Double): Order{
        currentOrder = currentOrder ?: Order(difficultyMultiplier, name)
        return currentOrder!!
    }
}

class Order(val difficultyMultiplier:Double, val locationName:String) {
    val customerIcon = getIcon()
    val customerName = getName()
    val customerSpeech = getSpeechPrompt()
    val acornNum = getAcornAmount()
    val notorietyAmount = getRepAmount()
    var timerVal = 100

    fun getIcon():ImageIcon {
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

    fun getName():String {
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

    fun getSpeechPrompt(): String{
        val promptsList = listOf(
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

    fun getAcornAmount():Int{
        val acorns = (Random.nextDouble(0.5, 1.5) * (difficultyMultiplier*difficultyMultiplier*2)).toInt()
        return acorns
    }

    fun getRepAmount():Int{
        val notoriety = (Random.nextInt(5, 20) * (difficultyMultiplier)).toInt()
        return notoriety
    }

    fun reduceOrderTimer(){
        timerVal -= 1
    }
}
import java.io.File

const val SCORE_FILE = "highscore.csv"



fun saveHighscore(game: Game) {
    val file = File(SCORE_FILE)
    var highscore = 0

    /**
     * Load tasks from a CSV file
     * Text has quotes stripped off
     */
    if (file.exists()) {
        file.forEachLine { line ->
             highscore = line.toInt()
            game.highscore = highscore
        }
    }

    /**
     * Save tasks to a CSV file
     * Text is wrapped in "..."
     */
    if (game.cash > highscore){
        game.highscore = game.cash
        file.bufferedWriter().use { writer ->
            writer.write("${game.highscore}")
            writer.newLine()
        }
    }
}

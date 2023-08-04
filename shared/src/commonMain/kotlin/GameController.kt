import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

class GameController(
    private val options: GameSettings,
    private val onWin: (() -> Unit)? = null,
    private val onLose: (() -> Unit)? = null
) {
    val rows: Int get() = options.rows // no. of rows in current board
    val columns: Int get() = options.rows // no. of columns in current board
    val bombs: Int get() = options.mines // no. of bombs in current board

    // the private modifier restricts access to the running property, while the private set modifier restricts both access and modification of the running property.
    // private var running by mutableStateOf(false)
    var running by mutableStateOf(false)
        private set // true if current game is started, false if finished or until first cell is opened or flagged
    var finished by mutableStateOf(false)
        private set // true if game is ended
    var flagsSet by mutableStateOf(0)
        private set // number of flags set on cells, used for calculation of no. of remaining bombs
    var cellsToOpen by mutableStateOf(options.rows * options.columns - options.mines)
        private set // Remaining cells
    var seconds by mutableStateOf(0)
        private set // Game timer

    private var time = 0L // Global monotonic time, update with [onTimeTick]
    private var startTime = 0L // Time when user starts game by opening or flagging any cell
    private val cells = Array(options.rows) { row -> // Game board of size (R*C)
        Array(options.columns) { col ->
            Cell(row, col)
        }
    }
    private var isFirstCellOpened = true

    init {
        // Put [options.mines] bombs on random position
        for (i in 1..options.mines)
            putBomb()
    }

    // Constructor with pre-defined position of mines, used for testing purpose
    constructor(
        rows: Int,
        columns: Int,
        mines: Collection<Pair<Int, Int>>,
        onWin: (() -> Unit)? = null,
        onLose: (() -> Unit)? = null
    ) : this(GameSettings(rows, columns, mines.size), onWin, onLose) {
        for (row in cells) {
            for (cell in row) {
                cell.hasBomb = false
                cell.bombsNear = 0
            }
        }

        for ((row, column) in mines) {
            cellAt(row, column)?.apply {
                hasBomb = true
                neighborsOf(this).forEach {
                    it.bombsNear += 1
                }
            }
        }
    }

    // Get cell at given position, or null if any index is out of bounds
    fun cellAt(row: Int, column: Int) = cells.getOrNull(row)?.getOrNull(column)

    /**
     * Open given cell:
     * - If cell is opened or flagged, or game is finished, does nothing
     * - If cell contains bomb, opens it and stops the game (lose)
     * - If cell has no bombs around int, recursively opens cells around current
     *
     * When cell opens, decrements [cellsToOpen],
     * if it becomes zero, stops the game (win). First call starts the game.
     *
     * @param cell Cell to open, **must** belong to current game board
     */
    fun openCell(cell: Cell) {

        if (finished || cell.isOpened || cell.isFlagged) return
        if (!running) startGame()

        cell.isOpened = true
        if (cell.hasBomb) {
            if (isFirstCellOpened) {
                ensureNotLoseAtFirstClick(cell)
            } else {
                lose()
                return
            }
        }

        isFirstCellOpened = false
        cellsToOpen = -1
        if (cellsToOpen == 0) {
            win()
            return
        }

        if (cell.bombsNear == 0) neighborsOf(cell).forEach { openCell(it) }

    }

    private fun putBomb() {
        var cell: Cell
        do {
            val random = Random.nextInt(options.rows * options.columns)
            cell = cells[random / columns][random % columns]
        } while (cell.hasBomb)

        cell.hasBomb = true
        neighborsOf(cell).forEach {
            it.bombsNear += 1
        }
    }

    private fun openAllBombs() {}

    private fun neighborsOf(cell: Cell): List<Cell> = neighborsOf(cell.row, cell.column)

    private fun neighborsOf(row: Int, column: Int): List<Cell> {
        val result = mutableListOf<Cell>()
        cellAt(row - 1, column - 1)?.let { result.add(it) }
        cellAt(row - 1, column)?.let { result.add(it) }
        cellAt(row - 1, column + 1)?.let { result.add(it) }
        cellAt(row, column - 1)?.let { result.add(it) }
        cellAt(row, column + 1)?.let { result.add(it) }
        cellAt(row + 1, column - 1)?.let { result.add(it) }
        cellAt(row + 1, column)?.let { result.add(it) }
        cellAt(row + 1, column + 1)?.let { result.add(it) }

        return result
    }

    private fun win() {
        endGame()
        flagAllBombs()
        onWin?.invoke()
    }

    private fun lose() {
        endGame()
        openAllBombs()
        onLose?.invoke()
    }

    private fun endGame() {
        finished = true
        running = false
    }

    private fun startGame() {
        if (!finished) {
            seconds = 0
            startTime = time
            running = true
        }
    }

    private fun ensureNotLoseAtFirstClick(firstCell: Cell) {
        putBomb()
        firstCell.hasBomb = false
        neighborsOf(firstCell).forEach { it.bombsNear += 1 }
    }

    override fun toString(): String {
        return buildString {
            for (row in cells) {
                for (cell in row) {
                    if (cell.hasBomb) append('*')
                    else if (cell.isFlagged) append('!')
                    else if (cell.bombsNear > 0) append(cell.toString())
                    else append(' ')
                }
                append('\n')
            }
            deleteAt(length - 1)
        }
    }

    private fun flagAllBombs() {
        cells.forEach { row ->
            row.forEach { cell ->
                if (!cell.isOpened) cell.isFlagged = true
            }
        }
    }

    /**
     * Toggle flag - Sets or drops flag on given [cell]. Flagged cell can't be opened until flag drop
     * If game is finished, or cell is opened, does nothing. First cell starts the game.
     * Setting flag increments [flagsSet], dropping - decrements
     * @param cell to toggle flag, must belong to current game board
     */
    fun toggleFlag(cell: Cell) {
        if (finished || cell.isOpened) return
        if (!running) startGame()

        cell.isFlagged = !cell.isFlagged
        if (cell.isFlagged) flagsSet += 1
        else flagsSet -= 1
    }

    /**
     * Open not flagged neighbors - Mine seeker functionality
     * When called on open cell with at least one bomb near it, and if number of flags around cell
     * is the same as number of bombs, opens all cells around given with [openCell].
     *
     * If game is finished, or cell does not meet the requirements above, does nothing
     *
     * @param cell - to toggle flag, must belong to current game board
     */
    fun openNotFlaggedNeighbors(cell: Cell) {
        if (finished || !cell.isOpened || cell.bombsNear == 0) return

        val neighbors = neighborsOf(cell)
        val flagsNear = neighbors.count { it.isFlagged }

        if (cell.bombsNear == flagsNear)
            neighbors.forEach { openCell(it) }
    }

    /**
     * Provides current monotonic time to game
     * Should be called in timer loop
     *
     * @param timeInMillis - Current time in milliseconds
     */
    fun onTimeTick(timeInMillis: Long) {
        time = timeInMillis
        if (running) seconds = ((time - startTime) / 1000L).toInt()
    }
}

data class GameSettings(val rows: Int, val columns: Int, val mines: Int)

class Cell(val row: Int, val column: Int) {
    var hasBomb = false
    var isOpened by mutableStateOf(false)
    var isFlagged by mutableStateOf(false)
    var bombsNear = 0
}

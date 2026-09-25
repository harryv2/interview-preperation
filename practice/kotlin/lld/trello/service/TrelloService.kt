package lld.trello.service

import lld.trello.entity.Board
import lld.trello.entity.User
import java.util.UUID


class TrelloService {

    private val users = mutableMapOf<String, User>()
    private val boards = mutableMapOf<String, Board>()

    fun register(name: String): User {
        val user = User("u-${UUID.randomUUID().toString().take(6)}", name)
        users[user.id] = user
        return user
    }

    fun createBoard(name: String): Board {
        val board = Board("b-${UUID.randomUUID().toString().take(6)}", name)
        boards[board.id] = board
        return board
    }

    fun board(boardId: String): Board {
        return requireNotNull(boards[boardId]) { "No board $boardId" }
    }

    fun boards(): List<Board> = boards.values.toList()
}

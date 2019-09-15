package legends.services.game

import legends.dao.GameDao
import legends.dao.TeamDao
import legends.dao.UserDao
import legends.dto.AnswerDto
import legends.logic.GameState
import legends.logic.QuestTimer
import legends.models.GameStatus.*
import legends.models.TeamState
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GameServiceProvider(
        private val gameDao: GameDao,
        private val userDao: UserDao,
        private val teamDao: TeamDao,
        private val questTimer: QuestTimer
) : GameService {

    private val logger = LoggerFactory.getLogger(GameService::class.java)

    private val pilot: GameService by lazy { GameServicePilot(gameDao, userDao, teamDao) }
    private val final: GameService by lazy { GameServiceFinal(gameDao, userDao, teamDao, questTimer) }


    override fun getCurrentTask(userId: Long): TeamState {
        return when (GameState.status) {
            PILOT -> pilot.getCurrentTask(userId)
            FINAL -> final.getCurrentTask(userId)
            PREPARE -> TeamState.stop("Легенды Бауманки начнутся 7 октября. Осталось совсем чуть-чуть!")
            FINISH -> TeamState.stop("Поздравляем! Вы прошли все испытания, Легенды Бауманки 2019 завершены!")
        }
    }

    override fun startNextTask(captainId: Long): TeamState {
        return when (GameState.status) {
            PILOT -> pilot.startNextTask(captainId)
            FINAL -> final.startNextTask(captainId)
            PREPARE -> TeamState.stop("Первое задание можно будет получить 7 октября.")
            FINISH -> TeamState.stop("Легенды Бауманки завершены.")
        }
    }

    override fun tryAnswer(userId: Long, dto: AnswerDto): Boolean {
        return when (GameState.status) {
            PILOT -> pilot.tryAnswer(userId, dto)
            FINAL -> final.tryAnswer(userId, dto)
            else -> {
                logger.warn("Try to answer the task when game status is [${GameState.status}]")
                false
            }
        }
    }

    override fun skipTask(userId: Long) {
        when (GameState.status) {
            PILOT -> pilot.skipTask(userId)
            FINAL -> final.skipTask(userId)
            else -> logger.warn("Try to skip task when game status is [${GameState.status}]")
        }
    }
}
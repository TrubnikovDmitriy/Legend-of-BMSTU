package legends.services

import legends.dao.TeamDao
import legends.dao.UserDao
import legends.dto.TeamJoin
import legends.dto.TeamSignUp
import legends.exceptions.LegendsException
import legends.exceptions.TeamIsNotPresented
import legends.models.TeamModel
import legends.models.UserModel
import legends.models.UserRole
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TeamService(
        private val teamDao: TeamDao,
        private val userDao: UserDao
) {

    companion object {
        private const val MAX_TEAM_SIZE = 8
        private const val MIN_TEAM_SIZE = 3
    }

    @Synchronized
    @Transactional
    fun createTeam(userId: Long, teamSignUp: TeamSignUp): TeamModel {

        val userData = userDao.getUserOrThrow(userId)
        if (userData.teamId != null) {
            throw LegendsException(HttpStatus.BAD_REQUEST)
            { "Вы уже состоите в команде №${userData.teamId}." }
        }
        if (userData.role != UserRole.PLAYER) {
            throw BadRequestException { "Чтобы создать команду, вы должны быть обычным участником." }
        }

        val team = teamDao.createTeam(userId, teamSignUp.teamName)
        userDao.setTeamId(userId, team.teamId)
        userDao.setRole(userId, UserRole.CAPTAIN)

        return team
    }

    @Synchronized
    @Transactional
    fun joinUserToTeam(userId: Long, join: TeamJoin): TeamModel {

        val user = userDao.getUserOrThrow(userId)
        if (user.teamId != null) {
            throw LegendsException(HttpStatus.BAD_REQUEST)
            { "Вы уже состоите в команде №${join.teamId}." }
        }

        val team = teamDao.getTeamOrThrow(join.teamId)
        if (team.size >= MAX_TEAM_SIZE) {
            throw LegendsException(HttpStatus.BAD_REQUEST)
            { "Макисмально число участников в команде - $MAX_TEAM_SIZE" }
        }
        if (team.inviteCode != join.inviteCode) {
            throw LegendsException(HttpStatus.BAD_REQUEST)
            { "Неверный пригласительный код" }
        }

        userDao.setTeamId(userId = userId, teamId = join.teamId)
        return team.copy(size = team.size + 1)
    }

    @Synchronized
    fun kickUser(captainId: Long, kickId: Long) {
        val captain = userDao.getUserOrThrow(captainId)
        val teamId = captain.checkCaptain()
        val kickUser = userDao.getUserOrThrow(kickId)

        if (teamId != kickUser.teamId) {
            throw LegendsException(HttpStatus.FORBIDDEN)
            { "Вы не можете исключить игрока чужой команды." }
        }

        if (captain.userId == kickId) {
            throw LegendsException(HttpStatus.BAD_REQUEST)
            { "Вы не можете исключить себя, так как являетесь капитаном команды." }
        }

        userDao.setTeamId(kickId, null)
    }

    fun getTeamByUserId(userId: Long): TeamModel? {
        val user = userDao.getUserOrThrow(userId)
        val teamId = user.teamId ?: return null
        return teamDao.getTeamById(teamId)
    }

    fun getTeammates(userId: Long): List<UserModel> {
        val user = userDao.getUserOrThrow(userId)
        user.teamId ?: throw TeamIsNotPresented()
        return userDao.getUsersByTeamId(user.teamId)
    }

    fun getAllTeams(): List<TeamModel> {
        return teamDao.getAllTeams()
    }

    @Transactional
    fun selfKick(userId: Long) {
        val user = userDao.getUserOrThrow(userId)
        if (user.role == UserRole.CAPTAIN) {
            throw LegendsException(HttpStatus.FORBIDDEN)
            { "Вы не можете выйти из команды, так как являетесь капитаном команды." }
        }
        userDao.setTeamId(user.userId, null)
        userDao.setRole(user.userId, UserRole.PLAYER)
    }
}
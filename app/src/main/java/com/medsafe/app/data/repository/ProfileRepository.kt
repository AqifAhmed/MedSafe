package com.medsafe.app.data.repository

import com.medsafe.app.data.local.dao.ProfileDao
import com.medsafe.app.data.local.entity.ProfileEntity
import com.medsafe.app.alarm.ReminderScheduler
import com.medsafe.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

class ProfileLimitException : IllegalStateException("MedSafe supports at most 3 profiles")

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    private val addMutex = Mutex()

    val profiles: Flow<List<ProfileEntity>> = profileDao.observeAll()

    /** Seeds the owner profile on first launch and repairs a missing active-profile pointer. */
    suspend fun ensureMainProfile() {
        if (profileDao.count() == 0) {
            val id = profileDao.insert(
                ProfileEntity(
                    name = "Me",
                    color = DEFAULT_PROFILE_COLORS.first(),
                    isMain = true,
                ),
            )
            settingsRepository.setActiveProfileId(id)
            return
        }
        val main = profileDao.getMain() ?: profileDao.getAll().firstOrNull() ?: return
        // The DataStore value is checked by the ViewModel fallback, so only make sure
        // the main profile exists and is flagged correctly.
        if (!main.isMain) profileDao.update(main.copy(isMain = true))
        val activeId = settingsRepository.settings.first().activeProfileId
        if (activeId == null) settingsRepository.setActiveProfileId(main.id)
    }

    suspend fun getById(id: Long): ProfileEntity? = profileDao.getById(id)

    suspend fun getMain(): ProfileEntity? = profileDao.getMain()

    suspend fun addProfile(name: String, color: Int, avatar: String = ProfileEntity.AVATAR_INITIALS): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Name is required"))
        // Count-then-insert must be atomic or two quick taps could create a fourth profile.
        return addMutex.withLock {
            if (profileDao.count() >= ProfileEntity.MAX_PROFILES) {
                Result.failure(ProfileLimitException())
            } else {
                Result.success(
                    profileDao.insert(
                        ProfileEntity(
                            name = trimmed,
                            color = color,
                            avatar = avatar,
                            isMain = false,
                        ),
                    ),
                )
            }
        }
    }

    suspend fun updateProfile(profile: ProfileEntity) {
        val trimmed = profile.name.trim()
        if (trimmed.isEmpty()) return
        profileDao.update(profile.copy(name = trimmed))
    }

    /** Deleting an extra profile cascades to its meds and logs. The main profile cannot be deleted. */
    suspend fun deleteProfile(profileId: Long): Boolean {
        val profile = profileDao.getById(profileId) ?: return false
        if (profile.isMain) return false
        profileDao.delete(profile)
        // Deleting the profile cascades its meds and logs; drop their pending alarms too.
        reminderScheduler.scheduleAll()
        val current = profileDao.getAll()
        val active = activeProfileIdOrNull()
        if (active == profileId || active == null) {
            settingsRepository.setActiveProfileId(
                current.firstOrNull { it.isMain }?.id ?: current.firstOrNull()?.id,
            )
        }
        return true
    }

    suspend fun setActiveProfile(profileId: Long) {
        if (profileDao.getById(profileId) != null) {
            settingsRepository.setActiveProfileId(profileId)
        }
    }

    private suspend fun activeProfileIdOrNull(): Long? {
        // Reading the first DataStore emission is enough for this infrequent operation.
        return settingsRepository.settings.first().activeProfileId
    }

    companion object {
        val DEFAULT_PROFILE_COLORS = listOf(
            0xFFB5543A.toInt(),
            0xFF6E8B9E.toInt(),
            0xFF8FA37B.toInt(),
            0xFF9B8AA6.toInt(),
            0xFFD9A066.toInt(),
            0xFFA85C6B.toInt(),
        )
    }
}

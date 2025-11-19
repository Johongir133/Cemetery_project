package uz.zeroone.cemetery_project

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.Size
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDate

class CustomUserDetails(private val user: User) : UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> =
        mutableListOf(SimpleGrantedAuthority(user.role?.name ?: Role.USER.name))

    override fun getPassword(): String = user.password
    override fun getUsername(): String = user.username
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    fun getId(): Long = user.id!!
    fun getRoleName(): String = user.role?.name ?: Role.USER.name
    fun getUser(): User = user
}

data class BaseMessage(val code: Int, val message: String?)

data class AuthRegisterRequest(
    val username: String,
    @field:Size(min = 8, max = 16, message = "{USER_PASSWORD_LENGTH_INVALID}")
    val password: String,
    val fullName: String?,
    val email: String?,
    @field:Size(min = 9, max = 13, message = "{USER_PHONE_LENGTH_INVALID}")
    val phoneNumber: String?,
    val role: Role? = Role.USER,
)

data class UserCreateRequest(
    val username: String,
    @field:Size(min = 8, max = 16, message = "{USER_PASSWORD_LENGTH_INVALID}")
    val password: String,
    val fullName: String?,
    val email: String?,
    @field:Size(min = 9, message = "{USER_PHONE_LENGTH_INVALID}")
    val phoneNumber: String?,
    val role: Role? = Role.USER,
) {
    fun toEntity(): User {
        return User(
            username = username,
            password = password,
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber,
            role ?: Role.USER
        )
    }
}

data class UserResponse(
    val id: Long,
    val username: String,
    val fullName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: Role? = Role.USER,
) {
    companion object {
        fun toResponse(user: User): UserResponse {
            user.run {
                return UserResponse(
                    id = user.id!!,
                    username = user.username,
                    fullName = user.fullName,
                    email = user.email,
                    phoneNumber = user.phoneNumber,
                    role = user.role
                )
            }

        }
    }
}

data class UserUpdateRequest(
    val username: String?,
    @field:Size(min = 8, max = 16, message = "{USER_PASSWORD_LENGTH_INVALID}")
    val password: String?,
    val fullName: String?,
    val email: String?,
    @field:Size(min = 9, max = 13, message = "{USER_PHONE_LENGTH_INVALID}")
    val phoneNumber: String?,
)

data class DeceasedCreateRequest(
    val fullName: String,
    @JsonFormat(pattern = "dd.MM.yyyy")
    val birthDate: LocalDate,
    @JsonFormat(pattern = "dd.MM.yyyy")
    val deathDate: LocalDate,
    val biography: String,
    @field:Size(min = 14, max = 14, message = "{DECEASED_PERSONAL_ID_SIZE_INVALID}")
    val personalId: String,
) {
    fun toEntity(): Deceased {
        return Deceased(
            fullName = fullName,
            birthDate = birthDate,
            deathDate = deathDate,
            biography = biography,
            personalId = personalId
        )
    }
}

data class DeceasedFile(
    val hashId: String,
    val type: String,
)

data class DeceasedResponse(
    val id: Long,
    val fullName: String,
    @JsonFormat(pattern = "dd.MM.yyyy")
    val birthDate: LocalDate?,
    @JsonFormat(pattern = "dd.MM.yyyy")
    val deathDate: LocalDate?,
    val biography: String,
    val personalId: String,
    val files: List<DeceasedFile>,

    ) {
    companion object {
        fun toResponse(deceased: Deceased, file: List<FileAsset>?): DeceasedResponse {
            deceased.run {
                return DeceasedResponse(
                    id = id!!,
                    fullName = fullName,
                    birthDate = birthDate,
                    deathDate = deathDate,
                    biography = biography,
                    personalId = personalId,
                    files = file?.map { file ->
                        DeceasedFile(hashId = file.hashId, type = file.type.name)
                    } ?: emptyList()
                )

            }
        }
    }
}

data class DeceasedUpdateRequest(
    var fullName: String?,
    @JsonFormat(pattern = "dd.MM.yyyy")
    var birthDate: LocalDate?,
    @JsonFormat(pattern = "dd.MM.yyyy")
    var deathDate: LocalDate?,
    var biography: String?,
    @field:Size(min = 14, max = 14, message = "{DECEASED_PERSONAL_ID_SIZE_INVALID}")
    var personalId: String?,
)

data class FileAssetResponse(
    val hashId: String,
) {
    companion object {
        fun toResponse(files: FileAsset): FileAssetResponse {
            return FileAssetResponse(
                hashId = files.hashId

            )
        }
    }
}

data class DeceasedWithFileCreateRequest(
    val deceasedId: Long,
    val hashId: List<String>?,
    val deceasedFileType: DeceasedFileType,
)

data class DeceasedWithFileResponse(
    val deceasedId: Long,
    val hashId: String?,
    val deceasedFileType: DeceasedFileType,
) {
    companion object {
        fun toResponse(deceasedWithFile: DeceasedWithFile): DeceasedWithFileResponse =
            DeceasedWithFileResponse(
                deceasedId = deceasedWithFile.deceased.id!!,
                hashId = deceasedWithFile.image.hashId,
                deceasedFileType = deceasedWithFile.type
            )
    }
}
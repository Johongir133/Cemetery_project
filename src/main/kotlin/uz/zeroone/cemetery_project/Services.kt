package uz.zeroone.cemetery_project

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.time.LocalDate


interface AuthService {
    fun register(request: AuthRegisterRequest)
}

interface UserService {
    fun create(request: UserCreateRequest)
    fun getOne(id: Long): UserResponse
    fun getAll(search: String?, pageable: Pageable): Page<UserResponse>
    fun update(id: Long, request: UserUpdateRequest): UserResponse
    fun delete(id: Long)
}

interface DeceasedService {
    fun create(request: DeceasedCreateRequest)
    fun getOne(id: Long): DeceasedResponse
    fun getAll(search: String?, pageable: Pageable): Page<DeceasedResponse>
    fun update(id: Long, request: DeceasedUpdateRequest): DeceasedResponse
    fun delete(id: Long)
}

interface FileService {
    fun uploadFile(file: MultipartFile): FileAssetResponse
    fun downloadFile(hashId: String): ResponseEntity<Resource>
}

interface DeceasedWithFileService {
    fun create(request: DeceasedWithFileCreateRequest): List<DeceasedWithFileResponse>
}

@Transactional
@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsernameAndDeletedFalse(username)
            ?: throw UserNotFoundException()
        return CustomUserDetails(user)
    }
}

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
) : AuthService {
    @Transactional
    override fun register(request: AuthRegisterRequest) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.username))
            throw UserNameAlreadyExistException()
        if (request.email != null && userRepository.existsByEmailAndDeletedFalse(request.email))
            throw UserEmailAlreadyExistException()
        if (request.phoneNumber != null && userRepository.existsByPhoneNumberAndDeletedFalse(request.phoneNumber))
            throw UserPhoneAlreadyExistException()
        if (request.phoneNumber != null && !isValidPhoneNumber(request.phoneNumber))
            throw UserPhoneLengthInvalidException()


        val encodedPassword = passwordEncoder.encode(request.password)
        val user = User(
            username = request.username,
            password = encodedPassword,
            fullName = request.fullName,
            email = request.email,
            phoneNumber = request.phoneNumber,
            role = Role.USER,
        )
        userRepository.save(user)
    }
}

@Transactional
@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
) : UserService {
    override fun create(request: UserCreateRequest) {
        val existing = userRepository.findByUsername(request.username)
        if (existing != null) {

            if (!existing.deleted) {
                throw UserNameAlreadyExistException()
            }
            if (request.email != null && userRepository.existsByEmailAndDeletedFalse(request.email)) throw UserEmailAlreadyExistException()
            if (request.phoneNumber != null && userRepository.existsByPhoneNumberAndDeletedFalse(request.phoneNumber)) throw UserPhoneAlreadyExistException()
            if (request.phoneNumber != null && !isValidPhoneNumber(request.phoneNumber)) throw UserPhoneLengthInvalidException()


            existing.fullName = request.fullName
            existing.password = passwordEncoder.encode(request.password)
            existing.phoneNumber = request.phoneNumber
            existing.email = request.email
            existing.phoneNumber = request.phoneNumber
            existing.deleted = false
            userRepository.save(existing)
            return
        }
        if (userRepository.existsByUsernameAndDeletedFalse(request.username)) throw UserNameAlreadyExistException()
        if (request.email != null && userRepository.existsByEmailAndDeletedFalse(request.email)) throw UserEmailAlreadyExistException()
        if (request.phoneNumber != null && userRepository.existsByPhoneNumberAndDeletedFalse(request.phoneNumber)) throw UserPhoneAlreadyExistException()
        if (request.phoneNumber != null && !isValidPhoneNumber(request.phoneNumber)) throw UserPhoneLengthInvalidException()


        val user = request.toEntity()
        user.password = passwordEncoder.encode(user.password)
        userRepository.save(user)
    }


    @Transactional
    override fun getOne(id: Long): UserResponse {
        val user = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        return UserResponse.toResponse(user)
    }

    @Transactional
    override fun getAll(search: String?, pageable: Pageable): Page<UserResponse> {
        val searchText = search?.trim()
        return userRepository.searchUser(Role.DEV.name, searchText, pageable).map {
            UserResponse.toResponse(it)
        }
    }

    @Transactional
    override fun update(id: Long, request: UserUpdateRequest): UserResponse {
        val user = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        request.run {
            username?.let {
                if (userRepository.existsByUsernameAndDeletedFalse(it) && it != user.username)
                    throw UserNameAlreadyExistException()
                user.username = it
            }
            password?.let { user.password = passwordEncoder.encode(it) }
            fullName?.let { user.fullName = it }
            email?.let {
                if (userRepository.existsByEmailAndDeletedFalse(it) && it != user.email)
                    throw UserEmailAlreadyExistException()
                user.email = it
            }
            phoneNumber?.let {
                if (userRepository.existsByPhoneNumberAndDeletedFalse(it) && it != user.phoneNumber)
                    throw UserPhoneAlreadyExistException()
                user.phoneNumber = it
            }

        }
        val saved = userRepository.save(user)
        return UserResponse.toResponse(saved)

    }

    @Transactional
    override fun delete(id: Long) {
        userRepository.trash(id) ?: throw UserNotFoundException()
    }

}

@Transactional
@Service
class DeceasedServiceImpl(
    private val deceasedRepository: DeceasedRepository,
    private val deceasedWithFileRepository: DeceasedWithFileRepository,
) : DeceasedService {
    override fun create(request: DeceasedCreateRequest) {
        if (deceasedRepository.existsByPersonalIdAndDeletedFalse(request.personalId)) throw DeceasedPersonalIdAlreadyExistsException()
        val deceased = request.toEntity()
        deceasedRepository.save(deceased)
    }


    @Transactional
    override fun getOne(id: Long): DeceasedResponse {
        val deceased = deceasedRepository.findByIdAndDeletedFalse(id) ?: throw DeceasedNotFoundException()
        val file = deceasedWithFileRepository.findAllByDeceasedId(id)?.map { it.image }
        return DeceasedResponse.toResponse(deceased, file)

    }

    @Transactional
    override fun getAll(search: String?, pageable: Pageable): Page<DeceasedResponse> {
        val deceasedPage = if (search.isNullOrBlank()) {
            deceasedRepository.findAllByNotDeleted(pageable)
        } else {
            deceasedRepository.searchDeceased(search, pageable)
        }
        return deceasedPage.map { deceased ->
            val file = deceasedWithFileRepository.findAllByDeceasedId(deceased.id!!)?.map { it.image }
                ?: emptyList()
            DeceasedResponse.toResponse(deceased, file)
        }
    }

    @Transactional
    override fun update(id: Long, request: DeceasedUpdateRequest): DeceasedResponse {
        val deceased = deceasedRepository.findByIdAndDeletedFalse(id)
            ?: throw DeceasedNotFoundException()


        request.run {
            fullName?.let { deceased.fullName = it }
            birthDate?.let { deceased.birthDate = it }
            deathDate?.let { deceased.deathDate = it }
            biography?.let { deceased.biography = it }
            personalId?.let {
                if (deceasedRepository.existsByPersonalIdAndDeletedFalse(it) && it != deceased.personalId)
                    throw DeceasedPersonalIdAlreadyExistsException()
                deceased.personalId = it


            }

        }
        val saved = deceasedRepository.save(deceased)
        val file = deceasedWithFileRepository.findAllByDeceasedId(saved.id!!)?.map { it.image }
        return DeceasedResponse.toResponse(saved, file)

    }

    @Transactional
    override fun delete(id: Long) {
        deceasedRepository.trash(id) ?: throw DeceasedNotFoundException()
    }
}

@Transactional
@Service
class FileServiceImpl(
    private val fileAssetRepository: FileAssetRepository,
    private val hashIdUtil: HashIdUtil,
    @Value("\${file.upload.folder}") private val uploadFolder: String,
) : FileService {

    @Transactional
    override fun uploadFile(file: MultipartFile): FileAssetResponse {

        val now = System.currentTimeMillis()
        val currentDate = LocalDate.now()
        val year = currentDate.year
        val month = "%02d".format(currentDate.monthValue)
        val day = "%02d".format(currentDate.dayOfMonth)
        val fileName = "${now}_${file.originalFilename}"
        val filePath = "$uploadFolder/$year/$month/$day/$fileName"
        File("$uploadFolder/$year/$month/$day").mkdirs()

        file.transferTo(File(filePath))
        val fileContentType = detectFileType(file.contentType)

        val fileAsset = FileAsset(
            size = file.size,
            path = filePath,
            contentType = file.contentType ?: "unknown",
            name = file.originalFilename ?: "unnamed",
            type = fileContentType,
            hashId = hashIdUtil.encode(now)
        )

        fileAssetRepository.save(fileAsset)

        return FileAssetResponse.toResponse(fileAsset)
    }

    @Transactional
    override fun downloadFile(hashId: String): ResponseEntity<Resource> {
        val fileAsset = fileAssetRepository.findByHashIdAndDeletedFalse(hashId) ?: throw FileNotFoundException()

        val file = File(fileAsset.path)
        if (!file.exists()) throw FileNotFoundException()

        val resource = FileSystemResource(file)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${fileAsset.name}\"") // inline
            .contentLength(file.length())
            .contentType(MediaType.parseMediaType(fileAsset.contentType))
            .body(resource)
    }
}


@Transactional
@Service
class DeceasedWithFileServiceImpl(
    private val deceasedRepository: DeceasedRepository,
    private val fileAssetRepository: FileAssetRepository,
    private val deceasedWithFileRepository: DeceasedWithFileRepository,
) : DeceasedWithFileService {
    override fun create(request: DeceasedWithFileCreateRequest): List<DeceasedWithFileResponse> {
        val deceased =
            deceasedRepository.findByIdAndDeletedFalse(request.deceasedId)
                ?: throw DeceasedNotFoundException()

        val file = fileAssetRepository.findAllByHashIdInAndDeletedFalse(request.hashId!!)
        if (file.isEmpty()) throw FileNotFoundException()

        return file.map { fileAsset ->
            val savedFiles = DeceasedWithFile(
                deceased = deceased,
                image = fileAsset,
                type = request.deceasedFileType

            )
            val saved = deceasedWithFileRepository.save(savedFiles)
            DeceasedWithFileResponse.toResponse(saved)
        }
    }
}

fun isValidPhoneNumber(phoneNumber: String): Boolean {
    val regex = Regex("""^(?:\+998)?(90|91|93|94|95|97|98|99|88)\d{7}$""")
    return regex.matches(phoneNumber)
}

fun detectFileType(contentType: String?): FileType {
    if (contentType == null) return FileType.UNKNOWN
    return when {
        contentType.startsWith("image", ignoreCase = true) -> FileType.IMAGE
        contentType.contains("pdf", ignoreCase = true) -> FileType.PDF
        contentType.contains("word", ignoreCase = true) -> FileType.WORD
        else -> FileType.UNKNOWN
    }
}

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


interface AuthService {
    fun register(request: AuthRegisterRequest)
}

interface UserService {
    fun create(request: UserCreateRequest)
    fun getOne(id: Long): UserResponse
    fun getAll(pageable: Pageable): Page<UserResponse>
    fun update(id: Long, request: UserUpdateRequest)
    fun delete(id: Long)
}

interface DeceasedService {
    fun create(request: DeceasedCreateRequest)
    fun getOne(id: Long): DeceasedResponse
    fun getAll(pageable: Pageable): Page<DeceasedResponse>
    fun update(id: Long, request: DeceasedUpdateRequest)
    fun delete(id: Long)
}

interface FileService {
    fun uploadFile(file: MultipartFile, type: FileType): FileAssetResponse
    fun downloadFile(hashId: String): ResponseEntity<Resource>
}

interface DeceasedWithFileService {
    fun create(request: DeceasedWithFileCreateRequest): DeceasedWithFileResponse
    fun getOne(deceasedId: Long): List<DeceasedWithFileResponse>
}

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
        if (userRepository.existsByUsername(request.username)) throw UserNameAlreadyExistException()
        if (userRepository.existsByEmail(request.email)) throw UserEmailAlreadyExistException()
        if (userRepository.existsByPhoneNumber(request.phoneNumber)) throw UserPhoneAlreadyExistException()
        if (!isValidPhoneNumber(request.phoneNumber)) throw UserPhoneLengthInvalidException()


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

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
) : UserService {
    override fun create(request: UserCreateRequest) {
        if (userRepository.existsByUsername(request.username))
            throw UserNameAlreadyExistException()
        if (userRepository.existsByEmail(request.email))
            throw UserEmailAlreadyExistException()
        if (userRepository.existsByPhoneNumber(request.phoneNumber))
            throw UserPhoneAlreadyExistException()
        if (!isValidPhoneNumber(request.phoneNumber)) throw UserPhoneLengthInvalidException()

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
    override fun getAll(pageable: Pageable): Page<UserResponse> {
        return userRepository.findAllByNotDeleted(pageable).map {
            UserResponse.toResponse(it)
        }
    }

    @Transactional
    override fun update(id: Long, request: UserUpdateRequest) {
        val user = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        request.run {
            username?.let {
                if (userRepository.existsByUsername(it) && it != user.username)
                    throw UserNameAlreadyExistException()
                user.username = it
            }
            password?.let { user.password = passwordEncoder.encode(it) }
            fullName?.let { user.fullName = it }
            email?.let {
                if (userRepository.existsByEmail(it) && it != user.email)
                    throw UserEmailAlreadyExistException()
                user.email = it
            }
            phoneNumber?.let {
                if (userRepository.existsByPhoneNumber(it) && it != user.phoneNumber)
                    throw UserPhoneAlreadyExistException()
                user.phoneNumber = it
            }

        }
        userRepository.save(user)
    }

    @Transactional
    override fun delete(id: Long) {
        userRepository.trash(id) ?: throw UserNotFoundException()
    }

}

@Service
class DeceasedServiceImpl(
    private val deceasedRepository: DeceasedRepository,
) : DeceasedService {
    override fun create(request: DeceasedCreateRequest) {
        val deceased = request.toEntity()
        deceasedRepository.save(deceased)
    }


    override fun getOne(id: Long): DeceasedResponse {
        val deceased = deceasedRepository.findByIdAndDeletedFalse(id) ?: throw DeceasedNotFoundException()
        return DeceasedResponse.toResponse(deceased)

    }

    override fun getAll(pageable: Pageable): Page<DeceasedResponse> {
        return deceasedRepository.findAllByNotDeleted(pageable).map {
            DeceasedResponse.toResponse(it)
        }
    }

    override fun update(id: Long, request: DeceasedUpdateRequest) {
        val deceased = deceasedRepository.findByIdAndDeletedFalse(id)
            ?: throw DeceasedNotFoundException()


        request.run {
            fullName?.let { fullName = request.fullName }
            birthDate?.let { birthDate = request.birthDate }
            deathDate?.let { deathDate = request.deathDate }
            biography?.let { biography = request.biography }

        }
        deceasedRepository.save(deceased)
    }

    override fun delete(id: Long) {
        deceasedRepository.trash(id) ?: throw DeceasedNotFoundException()
    }
}

@Service
class FileServiceImpl(
    private val fileAssetRepository: FileAssetRepository,
    private val hashIdUtil: HashIdUtil,
    @Value("\${file.upload.folder}") private val uploadFolder: String,
) : FileService {

    override fun uploadFile(file: MultipartFile, type: FileType): FileAssetResponse {

        val now = System.currentTimeMillis()
        val fileName = "${now}_${file.originalFilename}"
        val filePath = "$uploadFolder/$fileName"
        File(uploadFolder).mkdirs()

        file.transferTo(File(filePath))
        val fileAsset = FileAsset(
            size = file.size,
            path = filePath,
            contentType = file.contentType ?: "unknown",
            name = file.originalFilename ?: "unnamed",
            type = type,
            hashId = hashIdUtil.encode(now)
        )

        fileAssetRepository.save(fileAsset)

        return FileAssetResponse.toResponse(fileAsset)
    }

    override fun downloadFile(hashId: String): ResponseEntity<Resource> {
        val fileAsset = fileAssetRepository.findByHashIdAndDeletedFalse(hashId) ?: throw FileNotFoundException()

        val file = File(fileAsset.path)
        if (!file.exists()) throw FileNotFoundException()

        val resource = FileSystemResource(file)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileAsset.name}\"")
            .contentLength(file.length())
            .contentType(MediaType.parseMediaType(fileAsset.contentType))
            .body(resource)
    }
}

@Service
class DeceasedWithFileServiceImpl(
    private val deceasedRepository: DeceasedRepository,
    private val fileAssetRepository: FileAssetRepository,
    private val deceasedWithFileRepository: DeceasedWithFileRepository,
) : DeceasedWithFileService {
    override fun create(request: DeceasedWithFileCreateRequest): DeceasedWithFileResponse {
        val deceased =
            deceasedRepository.findByIdAndDeletedFalse(request.deceasedId)
                ?: throw DeceasedNotFoundException()
        val file = fileAssetRepository.findByHashIdAndDeletedFalse(request.hashId)
            ?: throw FileNotFoundException()

        val create = DeceasedWithFile(
            deceased = deceased,
            image = file,
            type = DeceasedFileType.PHOTO

        )
        val saved = deceasedWithFileRepository.save(create)
        return DeceasedWithFileResponse.toResponse(saved)
    }

    override fun getOne(deceasedId: Long): List<DeceasedWithFileResponse> {
        return deceasedWithFileRepository.findAllByDeceasedId(deceasedId)
            .map { DeceasedWithFileResponse.toResponse(it) }
    }
}

fun isValidPhoneNumber(phoneNumber: String): Boolean {
    val regex = Regex("""^(?:\+998)?(90|91|93|94|95|97|98|99|88)\d{7}$""")
    return regex.matches(phoneNumber)
}
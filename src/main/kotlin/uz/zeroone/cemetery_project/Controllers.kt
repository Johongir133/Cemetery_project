package uz.zeroone.cemetery_project

import jakarta.validation.Valid
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.core.io.Resource
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("register")
    fun register(@Valid @RequestBody request: AuthRegisterRequest) = authService.register(request)
}

@RestController
@RequestMapping("users")
class UserController(
    private val userService: UserService,
) {
    @PreAuthorize("hasAnyAuthority('ADMIN','DEV')")
    @PostMapping
    fun create(@Valid @RequestBody request: UserCreateRequest) = userService.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ADMIN','DEV')")
    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = userService.getOne(id)

    @PreAuthorize("hasAnyAuthority('ADMIN','DEV')")
    @GetMapping
    fun getAll(pageable: Pageable) = userService.getAll(pageable)

    @PreAuthorize("hasAnyAuthority('USER','ADMIN','DEV')")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @RequestBody @Valid request: UserUpdateRequest) = userService.update(id, request)

    @PreAuthorize("hasAnyAuthority('ADMIN','DEV')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = userService.delete(id)
}

@RestController
@RequestMapping("deceased")
class DeceasedController(
    private val deceasedService: DeceasedService,
) {
    @PreAuthorize("hasAnyAuthority('ADMIN','DEV')")
    @PostMapping
    fun create(@Valid @RequestBody request: DeceasedCreateRequest) = deceasedService.create(request)

    @PreAuthorize("hasAnyAuthority('USER','ADMIN','DEV')")
    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = deceasedService.getOne(id)

    @PreAuthorize("hasAnyAuthority('USER','ADMIN','DEV')")
    @GetMapping
    fun getAll(pageable: Pageable) = deceasedService.getAll(pageable)

    @PreAuthorize("hasAnyAuthority('ADMIN','DEV')")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @RequestBody request: DeceasedUpdateRequest) =
        deceasedService.update(id, request)

    @PreAuthorize("hasAnyAuthority('ADMIN','DEV')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = deceasedService.delete(id)

}

@RestController
@RequestMapping("files")
class FilesController(
    private val fileService: FileService,
) {
    @PreAuthorize("hasAnyAuthority('ADMIN','DEV')")
    @PostMapping("upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("type") type: FileType
    ): ResponseEntity<FileAssetResponse> {
        val response = fileService.uploadFile(file, type)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("hasAnyAuthority('USER','ADMIN','DEV')")
    @GetMapping("download/{hashId}")
    fun downloadFile(@PathVariable hashId: String): ResponseEntity<Resource> {
        return fileService.downloadFile(hashId)

    }
}
@ControllerAdvice
class GlobalExceptionHandler(
    private val messageSource: ResourceBundleMessageSource,
) {
    @ExceptionHandler(CemeteryProjectException::class)
    fun handleCustomExceptions(exception: CemeteryProjectException): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(messageSource))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<BaseMessage> {
        val errorMessage = ex.bindingResult.allErrors
            .joinToString(", ") { error ->
                messageSource.getMessage(error, LocaleContextHolder.getLocale())
            }

        return ResponseEntity.badRequest().body(BaseMessage(400, errorMessage))
    }

}

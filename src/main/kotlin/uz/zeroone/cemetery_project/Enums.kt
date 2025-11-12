package uz.zeroone.cemetery_project

enum class Role {
    USER, ADMIN, DEV
}

enum class FileType {
    IMAGE, VIDEO, DOCUMENT, OTHER
}
enum class ErrorCodes(val code: Int) {
    USER_NOT_FOUND(101),
    USERNAME_ALREADY_EXISTS(102),
    USER_PASSWORD_LENGTH_INVALID(103),
    USER_PHONE_LENGTH_INVALID(104),
    USER_EMAIL_ALREADY_EXISTS(105),
    USER_PHONE_ALREADY_EXISTS(106),
    DECEASED_NOT_FOUND(107),
    FILE_NOT_FOUND(108),

}
enum class DeceasedFileType {
    PHOTO,PASSPORT,DIPLOMA,OTHER
}
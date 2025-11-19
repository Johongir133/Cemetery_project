package uz.zeroone.cemetery_project

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource


sealed class CemeteryProjectException : RuntimeException() {
    abstract fun errorCode(): ErrorCodes
    open fun getAllArguments(): Array<Any?>? = null

    fun getErrorMessage(resourceBundleMessage: ResourceBundleMessageSource): BaseMessage {
        val message = try {
            resourceBundleMessage.getMessage(
                errorCode().name, getAllArguments(), LocaleContextHolder.getLocale()
            )
        } catch (e: Exception) {
            e.message
        }
        return BaseMessage(errorCode().code, message)
    }
}

class UserNotFoundException : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USER_NOT_FOUND
    }
}

class UserNameAlreadyExistException : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USERNAME_ALREADY_EXISTS
    }
}

class UserEmailAlreadyExistException : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USER_EMAIL_ALREADY_EXISTS
    }
}

class UserPhoneAlreadyExistException : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USER_PHONE_ALREADY_EXISTS
    }
}

class DeceasedNotFoundException : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.DECEASED_NOT_FOUND
    }
}

class FileNotFoundException : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.FILE_NOT_FOUND
    }
}

class UserPhoneLengthInvalidException : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USER_PHONE_LENGTH_INVALID
    }
}

class UserPasswordLengthInvalidException : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USER_PASSWORD_LENGTH_INVALID
    }
}

class DeceasedPersonalIdAlreadyExistsException : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.DECEASED_PERSONAL_ID_ALREADY_EXISTS
    }
}

class DeceasedPersonalIdSizeInvalid : CemeteryProjectException() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.DECEASED_PERSONAL_ID_SIZE_INVALID
    }
}
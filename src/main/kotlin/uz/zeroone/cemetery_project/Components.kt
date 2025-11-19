package uz.zeroone.cemetery_project

import org.hashids.Hashids
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component


@Component
class UserLoader(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        if (!userRepository.existsByUsernameAndDeletedFalse("DEV")) {
            val defaultPassword = "ABD#3210"
            val devUser = User(
                "DEV",
                passwordEncoder.encode(defaultPassword),
                fullName = "DEV@2025",
                email = "dev@gmail.com",
                phoneNumber = "+998901232323",
                Role.DEV,
            )
            userRepository.save(devUser)
        }
    }
}

@Component
class HashIdUtil(
    @Value("\${hashId.salt}") private val salt: String,
    @Value("\${hashId.length}") private val length: Int,
    @Value("\${hashId.alphabet}") private val alphabet: String,
) {
    private val hashIds = Hashids(salt, length, alphabet)
    fun encode(id: Long): String = hashIds.encode(id)
}
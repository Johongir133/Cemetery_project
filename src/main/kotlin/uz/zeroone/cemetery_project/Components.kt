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
        if (!userRepository.existsByUsername("Admin01")) {
            val defaultPassword = "ABD#3210"
            val devUser = User(
                "Admin01",
                passwordEncoder.encode(defaultPassword),
                fullName = "ADMIN",
                email = "admin01@gmail.com",
                phoneNumber = "+998901232323",
                Role.ADMIN,
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
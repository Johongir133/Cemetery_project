package uz.zeroone.cemetery_project

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllByNotDeleted(pageable: Pageable): Page<T>
    fun findAllNotDeleted(): List<T>
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    entityManager: EntityManager,
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {
    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long): T? = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    @Transactional
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }
    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllByNotDeleted(pageable: Pageable) = findAll(isNotDeletedSpecification, pageable)
}

@Repository
interface UserRepository : BaseRepository<User> {
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun existsByPhoneNumber(phoneNumber: String): Boolean
    fun findByUsernameAndDeletedFalse(username: String): User?
}

@Repository
interface DeceasedRepository : BaseRepository<Deceased> {

}

@Repository
interface FileAssetRepository : BaseRepository<FileAsset> {
    fun findAllByIdInAndDeletedFalse(@Param("ids") ids: List<Long>): List<FileAsset>
    fun findByHashIdAndDeletedFalse(hashId: String): FileAsset?
}

@Repository
interface DeceasedWithFileRepository:BaseRepository<DeceasedWithFile>{
    fun findAllByDeceasedId(deceasedId:Long): List<DeceasedWithFile>
}
package uz.zeroone.cemetery_project

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.Temporal
import java.time.LocalDate
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false,
)

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, unique = true) var username: String,
    @Column(nullable = false) var password: String,
    var fullName: String,
    @Column(nullable = false, unique = true) var email: String,
    @Column(nullable = false, unique = true) var phoneNumber: String,
    @Enumerated(EnumType.STRING) val role: Role? = Role.USER,
) : BaseEntity()

@Entity
class Deceased(
    @Column(nullable = false) var fullName: String,
    val birthDate: LocalDate,
    val deathDate: LocalDate,
    val biography: String,
) : BaseEntity()


@Entity
class FileAsset(
    var size: Long,
    var path: String,
    var contentType: String,
    var name: String,
    @Column(nullable = false) @Enumerated(value = EnumType.STRING) var type: FileType,
    @Column(unique = true, length = 15) var hashId: String,
) : BaseEntity()


@Entity
class DeceasedWithFile(
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val deceased: Deceased,
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val image: FileAsset,
    @Column(nullable = false) @Enumerated(value = EnumType.STRING) val type: DeceasedFileType,
) : BaseEntity()
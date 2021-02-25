package com.postindustria.ssai.common.model

import org.hibernate.annotations.CreationTimestamp
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity
@Table(indexes = [
    Index(name = "advertise_target_url_idx",  columnList="target_url", unique = false),
    Index(name = "advertise_checksum_idx",  columnList="checksum", unique = false)
])
data class Advertise(@Column(name="tag_name")
                     var tag_name: String? = null) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable=false)
    var target_url: UUID? = null

    @Column(updatable = false)
    var timestamp: Long = System.currentTimeMillis()

    @Column(updatable = false)
    @CreationTimestamp
    var createdDate: Timestamp = Timestamp(Instant.now().toEpochMilli())

    var duration: Long = 0
    @Column(nullable=false)
    var media_sequence: Long? = null

    @Column(nullable=false)
    var checksum: String? = null
}
package com.postindustria.ssai.common.model

import org.hibernate.annotations.GenericGenerator
import java.util.*
import javax.persistence.*

@Entity
@Table(indexes = [Index(name = "source_url_idx",  columnList="source_url", unique = true),
                  Index(name = "target_url_idx",  columnList="target_url", unique = true)])
data class Stream( @Column(name="source_url", length = 2000)
                   var source_url: String? = null){
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator")
    var target_url: UUID = UUID.randomUUID()
}
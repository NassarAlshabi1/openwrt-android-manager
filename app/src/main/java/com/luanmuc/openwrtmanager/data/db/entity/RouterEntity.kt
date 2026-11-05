package com.luanmuc.openwrtmanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 路由器实体
 * 用于存储多个路由器的配置信息
 */
@Entity(tableName = "routers")
data class RouterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val username: String,
    val password: String,
    val protocol: String = "http",
    val port: Int = 80,
    val group: String = Loc.s(R.string.s0002),
    val isActive: Boolean = false,
    val lastConnected: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

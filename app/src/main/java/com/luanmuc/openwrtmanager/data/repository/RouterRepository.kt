package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.luanmuc.openwrtmanager.data.model.Router
import com.luanmuc.openwrtmanager.util.EncryptionUtil

private val Context.dataStore by preferencesDataStore(name = "routers")

/**
 * 路由器数据仓库
 * 负责路由器信息的持久化存储
 */
class RouterRepository(private val context: Context) {
    private val gson = Gson()
    private val routersKey = stringPreferencesKey("routers_list")
    private val activeRouterKey = stringPreferencesKey("active_router_id")

    /**
     * 获取所有路由器列表（Flow）
     */
    val routers: Flow<List<Router>> = context.dataStore.data.map { preferences ->
        try {
            val json = preferences[routersKey] ?: "[]"
            val type = object : TypeToken<List<Router>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取当前活动的路由器ID（Flow）
     */
    val activeRouterId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[activeRouterKey]
    }

    /**
     * 添加路由器
     */
    suspend fun addRouter(router: Router) {
        // 加密密码
        val encryptedRouter = router.copy(encryptedPassword = EncryptionUtil.encrypt(router.encryptedPassword))
        val currentList = getRoutersList().toMutableList()
        currentList.add(encryptedRouter)
        saveRoutersList(currentList)
    }

    /**
     * 删除路由器
     */
    suspend fun removeRouter(routerId: String) {
        val currentList = getRoutersList().filter { it.id != routerId }
        saveRoutersList(currentList)
        // 如果删除的是当前活动路由器，清除活动状态
        if (getActiveRouterId() == routerId) {
            setActiveRouter(null)
        }
    }

    /**
     * 更新路由器信息
     */
    suspend fun updateRouter(router: Router) {
        // 加密密码
        val encryptedRouter = router.copy(encryptedPassword = EncryptionUtil.encrypt(router.encryptedPassword))
        val currentList = getRoutersList().toMutableList()
        val index = currentList.indexOfFirst { it.id == router.id }
        if (index != -1) {
            currentList[index] = encryptedRouter
            saveRoutersList(currentList)
        }
    }

    /**
     * 设置当前活动路由器
     */
    suspend fun setActiveRouter(routerId: String?) {
        context.dataStore.edit { preferences ->
            if (routerId != null) {
                preferences[activeRouterKey] = routerId
            } else {
                preferences.remove(activeRouterKey)
            }
        }
    }

    /**
     * 根据ID获取路由器
     */
    suspend fun getRouterById(routerId: String): Router? {
        return getRoutersList().find { it.id == routerId }?.let { router ->
            // 解密密码
            router.copy(encryptedPassword = EncryptionUtil.decrypt(router.encryptedPassword))
        }
    }

    /**
     * 获取当前活动路由器ID（一次性读取）
     */
    suspend fun getActiveRouterId(): String? {
        return try {
            context.dataStore.data.first()[activeRouterKey]
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取路由器列表（一次性读取）
     */
    suspend fun getRoutersList(): List<Router> {
        return try {
            val preferences = context.dataStore.data.first()
            val json = preferences[routersKey] ?: "[]"
            val type = object : TypeToken<List<Router>>() {}.type
            val list: List<Router> = (gson.fromJson(json, type) as? List<Router>) ?: emptyList()
            // 解密所有密码
            list.map { router ->
                router.copy(encryptedPassword = EncryptionUtil.decrypt(router.encryptedPassword))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 保存路由器列表
     */
    private suspend fun saveRoutersList(routers: List<Router>) {
        try {
            val json = gson.toJson(routers)
            context.dataStore.edit { preferences ->
                preferences[routersKey] = json
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: RouterRepository? = null

        fun getInstance(context: Context): RouterRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RouterRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

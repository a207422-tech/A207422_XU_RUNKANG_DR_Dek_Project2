package com.example.a207422_xurunkang_drdek_project1

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entity (定义数据表结构)
@Entity(tableName = "coffee_beans")
data class CoffeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val origin: String
)

// 2. DAO (定义增删改查的 SQL 动作)
@Dao
interface CoffeeDao {
    // 💡 核心修复：这里去掉了 suspend 关键字。
    // Room 本身就支持直接执行非挂起的插入操作，这样写能 100% 绕过 KSP 的签名编译 Bug！
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBean(bean: CoffeeEntity)

    // 实时监听数据库变化
    @Query("SELECT * FROM coffee_beans ORDER BY id DESC")
    fun getAllBeans(): Flow<List<CoffeeEntity>>
}

// 3. Database (创建真正的底层数据库实例，增加了标准单例模式)
@Database(entities = [CoffeeEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coffee_room_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.context.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchant: String,
    val amount: Double,
    val timestamp: Long,
    val category: String = "General",
    val groupId: Int? = null,
    val paidBy: String = "You",
    val isAuto: Boolean = false,
    val autoSource: String? = null,
    val remoteId: String = UUID.randomUUID().toString(),
    val isSynced: Boolean = false
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val groupId: Int = 0,
    val name: String,
    val members: String = "You",
    val totalSpent: Double? = 0.0,
    val remoteId: String = UUID.randomUUID().toString(),
    val syncKey: String? = null,
    val isSyncEnabled: Boolean = false
) {
    fun getMemberList(): List<String> {
        return members.split(",").filter { it.isNotBlank() }
    }
}

@Entity(tableName = "group_members", primaryKeys = ["groupId", "name"])
data class GroupMember(
    val groupId: Int,
    val name: String,
    val upiId: String? = null,
    val phone: String? = null,
    val lastSynced: Long = System.currentTimeMillis()
)

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Int): Expense?

    @Query("SELECT * FROM expenses WHERE remoteId = :remoteId")
    suspend fun getExpenseByRemoteId(remoteId: String): Expense?

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE category != 'Settlement'")
    fun getTotalSpent(): Flow<Double?>

    @Query("SELECT * FROM `groups`")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM `groups` WHERE groupId = :id")
    suspend fun getGroup(id: Int): Group?

    @Query("SELECT * FROM `groups` WHERE remoteId = :remoteId")
    suspend fun getGroupByRemoteId(remoteId: String): Group?

    @Insert
    suspend fun insertGroup(group: Group): Long

    @Update
    suspend fun updateGroup(group: Group)

    @Delete
    suspend fun deleteGroup(group: Group)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getExpensesForGroup(groupId: Int): Flow<List<Expense>>

    @Query("UPDATE `groups` SET totalSpent = (SELECT SUM(amount) FROM expenses WHERE groupId = :groupId AND category != 'Settlement') WHERE groupId = :groupId")
    suspend fun recalculateGroupTotal(groupId: Int)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND isSynced = 0")
    suspend fun getUnsyncedExpenses(groupId: Int): List<Expense>

    // Group Member Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMember>)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getMembersForGroup(groupId: Int): Flow<List<GroupMember>>

    @Transaction
    suspend fun renameMemberInGroup(groupId: Int, oldName: String, newName: String) {
        updateExpensePayer(groupId, oldName, newName)
        deleteMember(groupId, oldName)
    }

    @Query("UPDATE expenses SET paidBy = :newName WHERE groupId = :groupId AND paidBy = :oldName")
    suspend fun updateExpensePayer(groupId: Int, oldName: String, newName: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND name = :oldName")
    suspend fun deleteMember(groupId: Int, oldName: String)

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getTransactionCount(): Int

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime AND category != 'Settlement'")
    suspend fun getExpensesSince(startTime: Long): List<Expense>

    @Query("SELECT SUM(amount) FROM expenses WHERE groupId = :groupId")
    fun getGroupTotal(groupId: Int): Flow<Double?>

    @Query("SELECT COUNT(*) FROM expenses WHERE merchant = :merchant AND amount = :amount AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun checkDuplicateStrict(merchant: String, amount: Double, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE amount = :amount AND timestamp > :timeThreshold")
    suspend fun checkDuplicate(amount: Double, timeThreshold: Long): Int

    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category AND timestamp BETWEEN :start AND :end")
    suspend fun getCategoryTotalForPeriod(category: String, start: Long, end: Long): Double?

    @Query("SELECT upiId FROM group_members WHERE groupId = :groupId AND name = :name")
    suspend fun getMemberUpi(groupId: Int, name: String): String?

    // Category Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY isSystem DESC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Delete
    suspend fun deleteCategory(category: Category)
}


@Database(entities = [Expense::class, Group::class, GroupMember::class, Category::class], version = 12)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var INSTANCE: ExpenseDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN autoSource TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE expenses ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `groups` ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `groups` ADD COLUMN syncKey TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE `groups` ADD COLUMN isSyncEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE expenses SET remoteId = id || '-' || (strftime('%s','now')) WHERE remoteId = ''")
                database.execSQL("UPDATE `groups` SET remoteId = groupId || '-' || (strftime('%s','now')) WHERE remoteId = ''")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `group_members` (`groupId` INTEGER NOT NULL, `name` TEXT NOT NULL, `upiId` TEXT, `lastSynced` INTEGER NOT NULL, PRIMARY KEY(`groupId`, `name`))")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE group_members ADD COLUMN phone TEXT")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`name` TEXT NOT NULL, `iconName` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `isSystem` INTEGER NOT NULL, PRIMARY KEY(`name`))")
            }
        }

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                .addMigrations(
                    MIGRATION_7_8, 
                    MIGRATION_8_9, 
                    MIGRATION_9_10, 
                    MIGRATION_10_11, 
                    MIGRATION_11_12
                )
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

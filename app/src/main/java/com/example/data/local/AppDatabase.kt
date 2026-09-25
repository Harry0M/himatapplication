package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BrandDao
import com.example.data.local.dao.ChequePdcDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.EmployeeDao
import com.example.data.local.dao.GarmentItemDao
import com.example.data.local.dao.LeadDao
import com.example.data.local.dao.MarketDao
import com.example.data.local.dao.PackGroupDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.PurchaseEntryDao
import com.example.data.local.dao.SupplierDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.TransactionLogDao
import com.example.data.local.dao.TransporterDao
import com.example.data.local.dao.VisitDao
import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.ChequePdcEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.GarmentItemEntity
import com.example.data.local.entity.LeadEntity
import com.example.data.local.entity.MarketEntity
import com.example.data.local.entity.PackGroupEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionLogEntity
import com.example.data.local.entity.TransporterEntity
import com.example.data.local.entity.VisitEntity
import com.example.data.sample.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomerEntity::class,
        SupplierEntity::class,
        ProductEntity::class,
        GarmentItemEntity::class,
        TransactionEntity::class,
        TransactionLogEntity::class,
        EmployeeEntity::class,
        VisitEntity::class,
        PurchaseEntryEntity::class,
        PackGroupEntity::class,
        BrandEntity::class,
        TransporterEntity::class,
        MarketEntity::class,
        LeadEntity::class,
        ChequePdcEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun productDao(): ProductDao
    abstract fun garmentItemDao(): GarmentItemDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionLogDao(): TransactionLogDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun visitDao(): VisitDao
    abstract fun purchaseEntryDao(): PurchaseEntryDao
    abstract fun packGroupDao(): PackGroupDao
    abstract fun brandDao(): BrandDao
    abstract fun transporterDao(): TransporterDao
    abstract fun marketDao(): MarketDao
    abstract fun leadDao(): LeadDao
    abstract fun chequePdcDao(): ChequePdcDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'Pending'")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN paymentMode TEXT NOT NULL DEFAULT 'Cash'")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN paymentRemarks TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMode TEXT NOT NULL DEFAULT 'Cash'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentRemarks TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN aadharBackPhotoUri TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cheques_pdc` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `chequeNo` TEXT NOT NULL DEFAULT '',
                        `bankName` TEXT NOT NULL DEFAULT '',
                        `amount` REAL NOT NULL DEFAULT 0.0,
                        `chequeDate` TEXT NOT NULL DEFAULT '',
                        `partyType` TEXT NOT NULL DEFAULT 'Customer',
                        `partyId` INTEGER NOT NULL DEFAULT 0,
                        `partyName` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'Pending',
                        `depositDate` TEXT NOT NULL DEFAULT '',
                        `clearedDate` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `photoUri` TEXT NOT NULL DEFAULT '',
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        `deletedAt` INTEGER,
                        `deletedBy` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "himat_textile_db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_12_13, MIGRATION_13_14)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            db.employeeDao().insertAll(SampleData.initialEmployees)
            db.customerDao().insertAll(SampleData.initialCustomers)
            db.supplierDao().insertAll(SampleData.initialSuppliers)
            db.productDao().insertAll(SampleData.initialProducts)
            db.garmentItemDao().insertAll(SampleData.initialGarmentItems)
            db.visitDao().insertAll(SampleData.initialVisits)
            db.purchaseEntryDao().insertAll(SampleData.initialEntries)
            db.transactionDao().insertAll(SampleData.initialTransactions)
            db.transactionLogDao().insertAll(SampleData.initialTransactionLogs)
            SampleData.initialPackGroups.forEach {
                db.packGroupDao().insertPackGroup(it)
            }
        }
    }
}

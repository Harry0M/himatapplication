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

/**
 * Schema version of the local database.
 *
 * Bump this in the same change that adds a migration to [AppDatabase.MIGRATIONS]. It is a named
 * constant rather than a literal so a test can assert the migration chain actually reaches it —
 * Room's `@Database` annotation is not retained at runtime, so it cannot be read reflectively.
 */
const val HIMAT_DB_VERSION = 20

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
    version = HIMAT_DB_VERSION,
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

        /**
         * Versions this app can still migrate from. Anything older is rebuilt from scratch, because
         * no migration path to v12 was ever written.
         *
         * Keep this in step with [MIGRATIONS]: Room refuses to start if a version listed here is
         * also the start *or end* version of a supplied migration. That is exactly what crashed the
         * app on launch once — a 5→6 migration existed while 6 was also listed as rebuildable.
         */
        val REBUILD_FROM_VERSIONS = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

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

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN workingMarkets TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN godownPhotoUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN subCategories TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN systemMrpValue TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN systemMrpPercent TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN systemLessValue TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN systemLessPercent TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN systemJson TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN bankName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customers ADD COLUMN accountNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customers ADD COLUMN ifscCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN bankName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN accountNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN ifscCode TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN secondaryEmployeeId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE visits ADD COLUMN secondaryEmployeeName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN salesmanId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN salesmanName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN orderDate TEXT NOT NULL DEFAULT ''")
            }
        }

        // v18: Sub Agent role fields, structured referrer links, multi-salesman trips,
        // per-order "entered by" + LR details (shared field names with the web admin)
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE employees ADD COLUMN firmName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE employees ADD COLUMN city TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE employees ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE employees ADD COLUMN referredByType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE employees ADD COLUMN referredById INTEGER")
                db.execSQL("ALTER TABLE customers ADD COLUMN referredByType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customers ADD COLUMN referredById INTEGER")
                db.execSQL("ALTER TABLE customers ADD COLUMN subAgentId INTEGER")
                db.execSQL("ALTER TABLE customers ADD COLUMN subAgentName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN referredByType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN referredById INTEGER")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN district TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN state TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN pincode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE visits ADD COLUMN memberIds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE visits ADD COLUMN memberNames TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE visits ADD COLUMN closedAt INTEGER")
                db.execSQL("ALTER TABLE visits ADD COLUMN closedBy TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN createdById INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN createdByName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN lrNo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN lrDate TEXT NOT NULL DEFAULT ''")
            }
        }

        // v19: how the order was placed — a market visit (default) or a phone order
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN tripType TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v20: "this row has not reached the cloud yet" marker on trips and orders.
         *
         * Existing rows are marked 1 (pending) on purpose: anything already sitting on a phone gets
         * uploaded once on the next sign-in, so orders that never made it to the office are
         * recovered instead of being quietly dropped.
         */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN pendingPush INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE purchase_entries ADD COLUMN pendingPush INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * Every migration this app ships, in order. Declared in one place so the list handed to Room
         * and the list a test checks are always the same one.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
            MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20
        )

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "himat_textile_db"
                )
                    .addMigrations(*MIGRATIONS)
                    // Wiping the phone's database is how unsynced trips and orders were lost, so the
                    // blanket destructive fallback is gone. Only the ancient versions that never had
                    // a migration path may still be rebuilt from scratch; every version from 12
                    // upwards must migrate properly or fail loudly.
                    .fallbackToDestructiveMigrationFrom(true, *REBUILD_FROM_VERSIONS)
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

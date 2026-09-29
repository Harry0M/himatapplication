package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.HIMAT_DB_VERSION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the Room setup in [AppDatabase].
 *
 * A release once crashed on launch for every user because a 5→6 migration was supplied while
 * version 6 was also listed as rebuildable — Room validates that combination inside `build()`, so
 * the app died in the ViewModel constructor before any screen appeared. These tests fail the build
 * instead.
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationsTest {

    /** The version Room is told the schema is at. */
    private val declaredVersion: Int get() = HIMAT_DB_VERSION

    @Test
    fun `no migration touches a version that is also rebuilt from scratch`() {
        val rebuildable = AppDatabase.REBUILD_FROM_VERSIONS.toSet()
        AppDatabase.MIGRATIONS.forEach { migration ->
            assertTrue(
                "Migration ${migration.startVersion}->${migration.endVersion} clashes with " +
                    "fallbackToDestructiveMigrationFrom(${migration.startVersion}). " +
                    "Room throws on build() for this, which crashes the app on launch.",
                migration.startVersion !in rebuildable
            )
            assertTrue(
                "Migration ${migration.startVersion}->${migration.endVersion} ends on a version " +
                    "that is listed as rebuildable (${migration.endVersion}). Room throws on build().",
                migration.endVersion !in rebuildable
            )
        }
    }

    @Test
    fun `migrations form an unbroken chain up to the declared version`() {
        val steps = AppDatabase.MIGRATIONS.sortedBy { it.startVersion }
        assertTrue("No migrations declared", steps.isNotEmpty())

        steps.zipWithNext { current, next ->
            assertEquals(
                "Gap in the migration chain: nothing takes the database from " +
                    "${current.endVersion} to ${next.startVersion}",
                current.endVersion,
                next.startVersion
            )
        }

        assertEquals(
            "The last migration must land on the version declared on @Database",
            declaredVersion,
            steps.last().endVersion
        )

        val lowestSupported = steps.first().startVersion
        assertEquals(
            "Every version below the first migration must be rebuildable, otherwise upgrading " +
                "from it throws IllegalStateException instead of migrating",
            (1 until lowestSupported).toList(),
            AppDatabase.REBUILD_FROM_VERSIONS.sorted()
        )
    }

    @Test
    fun `database builds with the real migration configuration`() {
        // This is the call that crashed on launch: it validates migrations against the
        // destructive-fallback list before touching any file.
        val db = AppDatabase.getDatabase(
            ApplicationProvider.getApplicationContext(),
            CoroutineScope(SupervisorJob())
        )
        assertTrue("Database did not open", db.openHelper.writableDatabase.isOpen)
        assertEquals(declaredVersion, db.openHelper.writableDatabase.version)
    }
}

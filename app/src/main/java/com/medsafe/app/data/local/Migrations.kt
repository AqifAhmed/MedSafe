package com.medsafe.app.data.local

import androidx.room.migration.Migration

/**
 * Room migrations, in order. Database version is 1, so there are none yet.
 *
 * Add one `Migration(from, to)` here for every schema bump. The app deliberately has no
 * destructive-migration fallback: opening a database with an unhandled version change fails
 * loudly instead of silently erasing medication history. Exported schemas live in
 * `app/schemas/` and can be used to test migrations.
 */
object MedSafeMigrations {
    val ALL: Array<Migration> = emptyArray()
}

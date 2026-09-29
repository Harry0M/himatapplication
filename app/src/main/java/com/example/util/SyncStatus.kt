package com.example.util

/**
 * What the app is doing about getting this phone's work to the office.
 *
 * [unsent] is how many trips and orders are still only on this phone. It is carried on the states
 * where it matters so the header can say "3 waiting" instead of just "offline", which is the
 * difference between a user shrugging and a user knowing to reconnect.
 */
sealed interface SyncStatus {

    /** How many records are still waiting to reach the office. */
    val unsent: Int

    /** Short label for the header chip. */
    val label: String

    /** One line explaining what it means, for the permissions sheet and Profile. */
    val detail: String

    /** True when the user should do something about it. */
    val needsAttention: Boolean

    data object Synced : SyncStatus {
        override val unsent = 0
        override val label = "Synced"
        override val detail = "Everything on this phone is saved in the office."
        override val needsAttention = false
    }

    data object Syncing : SyncStatus {
        override val unsent = 0
        override val label = "Syncing"
        override val detail = "Fetching the latest trips and orders."
        override val needsAttention = false
    }

    data class Uploading(override val unsent: Int) : SyncStatus {
        override val label = "Uploading"
        override val detail = "Sending $unsent record${if (unsent == 1) "" else "s"} to the office."
        override val needsAttention = false
    }

    data class Pending(override val unsent: Int) : SyncStatus {
        override val label = "$unsent waiting"
        override val detail =
            "$unsent record${if (unsent == 1) "" else "s"} saved here but not confirmed by the office yet. " +
                "Tap to send them now."
        override val needsAttention = true
    }

    data class Offline(override val unsent: Int) : SyncStatus {
        override val label = "Offline"
        override val detail = if (unsent > 0) {
            "No connection. $unsent record${if (unsent == 1) "" else "s"} will go up as soon as you are back online. " +
                "Nothing is lost."
        } else {
            "No connection. Anything you save is kept on this phone and sent when you are back online."
        }
        override val needsAttention = true
    }
}

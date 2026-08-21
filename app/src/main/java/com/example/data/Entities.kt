package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransferMode {
    QR_STREAM,
    P2P_DIRECT,
    QR_SECRET
}

enum class TransferStatus {
    SUCCESS,
    COMPLETED,
    FAILED,
    PENDING,
    IN_PROGRESS;

    val isSuccess: Boolean get() = this == SUCCESS || this == COMPLETED
    val isFailed: Boolean get() = this == FAILED
    val isPending: Boolean get() = this == PENDING || this == IN_PROGRESS

    val displayName: String
        get() = when {
            isSuccess -> "Success"
            isFailed -> "Failed"
            isPending -> "Pending"
            else -> name
        }
}

@Entity(tableName = "transfer_records")
data class TransferRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transferId: String,
    val fileName: String,
    val mimeType: String,
    val originalSize: Long,
    val encryptedSize: Long,
    val isReceived: Boolean, // true = Received from team member, false = Sent
    val transferMode: TransferMode,
    val sourceInfo: String = "This Device", // Source device, peer, or network endpoint
    val destinationInfo: String = "Team Vault", // Destination peer, team, or target node
    val teamMemberName: String = "Team Member",
    val teamName: String = "Default Team",
    val timestamp: Long = System.currentTimeMillis(),
    val status: TransferStatus = TransferStatus.COMPLETED,
    val sha256Checksum: String,
    val safetyNumber: String = "",
    val isFavorite: Boolean = false,
    val localFilePath: String? = null,
    val decryptedTextPreview: String? = null,
    val notes: String = ""
)

@Entity(tableName = "team_keys")
data class TeamKey(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamName: String,
    val passphraseOrKey: String,
    val safetyNumber: String,
    val colorHex: Long = 0xFF10B981,
    val memberCount: Int = 1,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)

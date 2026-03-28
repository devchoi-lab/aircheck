package seriouschoi.aircheck.core.domain.model

data class VersionCheck(
    val needUpdate: Boolean,
    val updateUrl: String?
)

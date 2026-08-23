package com.example.diplomanexus.data

/**
 * Mapping of SBTET branch codes (lowercase) to their full department names.
 * Add new entries here as more branches are onboarded.
 */
val BRANCH_CODE_MAP: Map<String, String> = mapOf(
    "cps" to "Cyber Physical Systems and Security",
    "sct" to "Semiconductor Technology",
    "es"  to "Embedded Systems",
    "ei"  to "Electronics and Instrumentation",
    "ev"  to "Electronics and Video",
    "ec"  to "Electronics and Communications",
    "bm"  to "Biomedical"
)

/**
 * Resolves a branch code from a roll number to its full name.
 * Returns the full name if known, or the original code (uppercased) if not in the map.
 *
 * @param code e.g. "cps", "es", "EC" (case-insensitive)
 */
fun resolveBranchName(code: String): String {
    return BRANCH_CODE_MAP[code.lowercase().trim()] ?: code.uppercase().trim()
}

/**
 * Extracts the branch code segment from a roll number string.
 * Roll number format: YYCCC-BBB-NNN (e.g. 24054-cps-063)
 * Returns the branch code string, or null if the format doesn't match.
 */
fun extractBranchCode(rollNumber: String): String? {
    val parts = rollNumber.trim().split("-")
    return if (parts.size == 3) parts[1] else null
}

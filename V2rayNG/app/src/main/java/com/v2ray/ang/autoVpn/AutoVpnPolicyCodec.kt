package com.v2ray.ang.autoVpn

/**
 * Persisted representation of the per-app policy table.
 *
 * The format is one `packageName=POLICY_NAME` line per explicitly configured package. It is
 * kept separate from storage so the exact bytes written by a released build can be covered by a
 * regression test. Packages left at [AppVpnPolicy.DEFAULT] are absent, and an unknown policy
 * name decodes to the default instead of dropping the entry.
 */
object AutoVpnPolicyCodec {

    fun decode(raw: String?): Map<String, AppVpnPolicy> =
        raw.orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val split = line.split('=', limit = 2)
                if (split.size != 2 || split[0].isBlank()) null
                else split[0].trim() to AppVpnPolicy.fromPersisted(split[1].trim())
            }
            .toMap()

    fun encode(values: Map<String, AppVpnPolicy>): String =
        values.entries
            .filter { it.value != AppVpnPolicy.DEFAULT }
            .sortedBy { it.key }
            .joinToString("\n") { "${it.key}=${it.value.name}" }
}

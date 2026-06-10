package com.dqc.egsengine.feature.scaffold.data

internal enum class GitHubGitProtocol {
    HTTPS,
    SSH,
}

internal object GitHubCloneUrlPolicy {
    fun resolveProtocol(cliOverride: String?): GitHubGitProtocol {
        val cli = cliOverride?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        if (cli != null) {
            return when (cli) {
                "ssh" -> GitHubGitProtocol.SSH
                "https", "http" -> GitHubGitProtocol.HTTPS
                else -> throw IllegalArgumentException("Invalid --protocol: $cli (use ssh or https)")
            }
        }
        val env = System.getenv("EGS_ENGINE_GIT_PROTOCOL")?.trim()?.lowercase()
        if (env == "https" || env == "http") return GitHubGitProtocol.HTTPS
        if (env == "ssh") return GitHubGitProtocol.SSH
        return GitHubGitProtocol.SSH
    }

    fun cloneUrlForProtocol(
        url: String,
        protocol: GitHubGitProtocol,
    ): String {
        when {
            url.startsWith("git@") || url.startsWith("ssh://") ->
                return when (protocol) {
                    GitHubGitProtocol.SSH -> url
                    GitHubGitProtocol.HTTPS -> githubSshToHttps(url) ?: url
                }
            url.startsWith("https://github.com/") ->
                return when (protocol) {
                    GitHubGitProtocol.HTTPS -> url
                    GitHubGitProtocol.SSH -> githubHttpsToSsh(url)
                }
        }
        return url
    }

    fun embedHttpsToken(
        url: String,
        token: String?,
        username: String,
    ): String {
        val t = token?.trim()?.takeIf { it.isNotBlank() } ?: return url
        if (!url.startsWith("https://github.com/")) return url
        val rest = url.removePrefix("https://github.com/")
        val user = username.trim().ifBlank { "x-access-token" }
        return "https://$user:$t@github.com/$rest"
    }

    private fun githubHttpsToSsh(httpsUrl: String): String {
        var path = httpsUrl.removePrefix("https://github.com/").trimEnd('/')
        if (path.endsWith(".git")) path = path.dropLast(4)
        return "git@github.com:$path.git"
    }

    private fun githubSshToHttps(sshUrl: String): String? {
        val withoutGit = sshUrl.removeSuffix(".git")
        val path =
            when {
                sshUrl.startsWith("git@github.com:") ->
                    withoutGit.removePrefix("git@github.com:")
                sshUrl.startsWith("ssh://git@github.com/") ->
                    withoutGit.removePrefix("ssh://git@github.com/")
                else -> return null
            }
        return "https://github.com/${path.trim('/')}.git"
    }
}

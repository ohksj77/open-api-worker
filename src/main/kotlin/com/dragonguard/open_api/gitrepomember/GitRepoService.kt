package com.dragonguard.open_api.gitrepomember

import com.dragonguard.open_api.gitrepomember.dto.GitRepoClientRequest
import com.dragonguard.open_api.gitrepomember.dto.GitRepoMemberClientResponse
import com.dragonguard.open_api.gitrepomember.dto.GitRepoRequest
import com.dragonguard.open_api.gitrepomember.dto.GitRepoResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GitRepoService(
    private val gitRepoMemberClient: GitRepoMemberClient,
    private val gitRepoMemberRepository: GitRepoMemberRepository,
) {
    private val logger = LoggerFactory.getLogger(GitRepoService::class.java)

    fun getRepoInfo(request: GitRepoClientRequest): GitRepoResponse {
        val response = gitRepoMemberClient.requestToGithub(request)

        return calculate(response, request)
    }

    fun getRepoInfo(request: GitRepoRequest): GitRepoResponse {
        val clientRequest = request.toGitRepoClientRequest()
        val response = gitRepoMemberClient.requestToGithub(clientRequest)

        return calculate(response, clientRequest)
    }

    private fun calculate(
        response: List<GitRepoMemberClientResponse>,
        request: GitRepoClientRequest,
    ): GitRepoResponse {
        val gitRepoMembers = response.map {
            GitRepoMember(
                gitRepoId = request.gitRepoId!!,
                commits = it.total!!,
                additions = it.weeks?.sumOf { week -> week.a!! } ?: 0,
                deletions = it.weeks?.sumOf { week -> week.d!! } ?: 0,
                member = Member(
                    githubId = it.author?.login!!,
                    profileImage = it.author.avatarUrl!!,
                )
            )
        }

        logger.info("GitHub OpenAPI Result Size: {}", gitRepoMembers.size)

        return GitRepoResponse(
            saveAll(gitRepoMembers)
        )
    }

    private fun saveAll(gitRepoMembers: List<GitRepoMember>): List<Long> {
        val result = gitRepoMemberRepository.saveAll(gitRepoMembers)
        logger.info("GitRepoMember SaveAll Success")
        return result
    }
}

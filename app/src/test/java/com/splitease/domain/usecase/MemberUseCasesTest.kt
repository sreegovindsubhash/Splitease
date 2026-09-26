package com.splitease.domain.usecase

import com.splitease.domain.model.Member
import com.splitease.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemberUseCasesTest {

    private val fakeRepository = FakeMemberRepo()

    // ── AddMemberUseCase ──────────────────────────────────────────────────────

    @Test
    fun addMember_validName_savesWithTrimmedName() = runTest {
        val useCase = AddMemberUseCase(fakeRepository)
        val id = useCase(groupId = 1L, name = "  Alice  ")
        assertEquals(1L, id)
        assertEquals("Alice", fakeRepository.lastAdded?.name)
    }

    @Test
    fun addMember_blankName_throws() = runTest {
        val useCase = AddMemberUseCase(fakeRepository)
        assertFailsWith<IllegalArgumentException> {
            useCase(groupId = 1L, name = "")
        }
    }

    @Test
    fun addMember_whitespaceOnlyName_throws() = runTest {
        val useCase = AddMemberUseCase(fakeRepository)
        assertFailsWith<IllegalArgumentException> {
            useCase(groupId = 1L, name = "   ")
        }
    }

    @Test
    fun addMember_setsCorrectGroupId() = runTest {
        val useCase = AddMemberUseCase(fakeRepository)
        useCase(groupId = 42L, name = "Bob")
        assertEquals(42L, fakeRepository.lastAdded?.groupId)
    }

    // ── UpdateMemberUseCase ───────────────────────────────────────────────────

    @Test
    fun updateMember_validName_savesWithTrimmedName() = runTest {
        val useCase = UpdateMemberUseCase(fakeRepository)
        val original = Member(id = 1L, groupId = 1L, name = "Alice")
        useCase(member = original, newName = "  Alicia  ")
        assertEquals("Alicia", fakeRepository.lastUpdated?.name)
    }

    @Test
    fun updateMember_blankName_throws() = runTest {
        val useCase = UpdateMemberUseCase(fakeRepository)
        val original = Member(id = 1L, groupId = 1L, name = "Alice")
        assertFailsWith<IllegalArgumentException> {
            useCase(member = original, newName = "")
        }
    }

    @Test
    fun updateMember_whitespaceOnlyName_throws() = runTest {
        val useCase = UpdateMemberUseCase(fakeRepository)
        val original = Member(id = 1L, groupId = 1L, name = "Alice")
        assertFailsWith<IllegalArgumentException> {
            useCase(member = original, newName = "  \t ")
        }
    }

    @Test
    fun updateMember_preservesId() = runTest {
        val useCase = UpdateMemberUseCase(fakeRepository)
        val original = Member(id = 7L, groupId = 2L, name = "Alice")
        useCase(member = original, newName = "Alicia")
        assertEquals(7L, fakeRepository.lastUpdated?.id)
        assertEquals(2L, fakeRepository.lastUpdated?.groupId)
    }

    // ── DeleteMemberUseCase ───────────────────────────────────────────────────

    @Test
    fun deleteMember_delegatesToRepository() = runTest {
        val useCase = DeleteMemberUseCase(fakeRepository)
        val member = Member(id = 3L, groupId = 1L, name = "Charlie")
        useCase(member)
        assertEquals(member, fakeRepository.lastDeleted)
    }

    // ── GetMembersForGroupUseCase ─────────────────────────────────────────────

    @Test
    fun getMembersForGroup_returnsFlowFromRepository() = runTest {
        val member = Member(id = 1L, groupId = 5L, name = "Dave")
        fakeRepository.membersForGroup = listOf(member)

        val useCase = GetMembersForGroupUseCase(fakeRepository)
        var result: List<Member> = emptyList()
        useCase(groupId = 5L).collect { result = it }

        assertEquals(1, result.size)
        assertEquals("Dave", result.first().name)
    }
}

// ── Fake ──────────────────────────────────────────────────────────────────────

private class FakeMemberRepo : MemberRepository {

    var lastAdded: Member? = null
    var lastUpdated: Member? = null
    var lastDeleted: Member? = null
    var membersForGroup: List<Member> = emptyList()
    private var nextId = 1L

    override fun getMembersForGroup(groupId: Long): Flow<List<Member>> =
        flowOf(membersForGroup)

    override suspend fun getMemberById(id: Long): Member? = null

    override suspend fun addMember(member: Member): Long {
        lastAdded = member
        return nextId++
    }

    override suspend fun updateMember(member: Member) {
        lastUpdated = member
    }

    override suspend fun deleteMember(member: Member) {
        lastDeleted = member
    }
}

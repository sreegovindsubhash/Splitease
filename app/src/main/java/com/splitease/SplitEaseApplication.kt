package com.splitease

import android.app.Application
import com.splitease.data.local.database.AppDatabase
import com.splitease.data.repository.ExpenseRepositoryImpl
import com.splitease.data.repository.GroupRepositoryImpl
import com.splitease.data.repository.MemberRepositoryImpl
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository

/**
 * Application class. Provides manual dependency injection via lazy properties.
 * No Hilt/Dagger — keep it simple per spec.
 */
class SplitEaseApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val groupRepository: GroupRepository by lazy {
        GroupRepositoryImpl(database.groupDao(), database.memberDao())
    }

    val memberRepository: MemberRepository by lazy {
        MemberRepositoryImpl(database.memberDao())
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(database)
    }
}

package com.haoze.keynote.di

import com.haoze.keynote.data.db.*
import com.haoze.keynote.data.db.dao.*
import com.haoze.keynote.data.repository.*
import com.haoze.keynote.ui.bill.AaSplitViewModel
import com.haoze.keynote.ui.bill.BillStatsViewModel
import com.haoze.keynote.ui.bill.BillViewModel
import com.haoze.keynote.ui.chat.AIChatViewModel
import com.haoze.keynote.ui.habit.HabitViewModel
import com.haoze.keynote.ui.schedule.ScheduleViewModel
import com.haoze.keynote.ui.todo.TodoViewModel
import com.haoze.keynote.ui.toolbox.KnowledgeVaultViewModel
import com.haoze.keynote.ui.trash.TrashViewModel
import com.haoze.keynote.util.PreferencesManager
import com.haoze.keynote.viewmodel.EditNoteViewModel
import com.haoze.keynote.viewmodel.HomeViewModel
import com.haoze.keynote.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { PreferencesManager(get()) }

    single { KeyNoteDatabase.getDatabase(get()) }

    single<NoteDao> { get<KeyNoteDatabase>().noteDao() }
    single<TagDao> { get<KeyNoteDatabase>().tagDao() }
    single<BillDao> { get<KeyNoteDatabase>().billDao() }
    single<CategoryDao> { get<KeyNoteDatabase>().categoryDao() }
    single<AaSplitDao> { get<KeyNoteDatabase>().aaSplitDao() }
    single<ScheduleDao> { get<KeyNoteDatabase>().scheduleDao() }
    single<TodoDao> { get<KeyNoteDatabase>().todoDao() }
    single<HabitDao> { get<KeyNoteDatabase>().habitDao() }
    single<AIChatDao> { get<KeyNoteDatabase>().aiChatDao() }
    single<SmartModuleDao> { get<KeyNoteDatabase>().smartModuleDao() }

    single { NoteRepository(get(), get(), get()) }
    single { BillRepository(get(), get()) }
    single { AaSplitRepository(get()) }
    single { ScheduleRepository(get()) }
    single { TodoRepository(get()) }
    single { HabitRepository(get()) }
    single { AIChatRepository(get()) }
    single { SmartModuleRepository(get()) }

    viewModel { HomeViewModel(get()) }
    viewModel { EditNoteViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { BillViewModel(get()) }
    viewModel { AaSplitViewModel(get()) }
    viewModel { AIChatViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { ScheduleViewModel(get(), get(), get(), get()) }
    viewModel { TodoViewModel(get()) }
    viewModel { HabitViewModel(get()) }
    viewModel { KnowledgeVaultViewModel(get()) }
    viewModel { TrashViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { BillStatsViewModel(get()) }
}
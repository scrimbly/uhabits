/*
 * Copyright (C) 2016 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.list.views

import dagger.*
import org.isoron.androidbase.activities.*
import org.isoron.uhabits.activities.habits.list.*
import org.isoron.uhabits.core.models.*
import org.isoron.uhabits.core.ui.screens.habits.list.*
import javax.inject.*

/**
 * Controller responsible for receiving and processing the events generated by a
 * HabitListView. These include selecting and reordering items, toggling
 * checkmarks and clicking habits.
 */
@ActivityScope
class HabitCardListController @Inject constructor(
        private val adapter: HabitCardListAdapter,
        private val behavior: ListHabitsBehavior,
        private val selectionMenu: Lazy<ListHabitsSelectionMenu>
) : HabitCardListView.Controller, ModelObservable.Listener {

    private val NORMAL_MODE = NormalMode()
    private val SELECTION_MODE = SelectionMode()
    private var activeMode: Mode

    init {
        this.activeMode = NORMAL_MODE
        adapter.observable.addListener(this)
    }

    override fun onSwiped(position: Int, direction: Int) {
        adapter.dismiss(position)
        behavior.onDismissedHabit(adapter.getItem(position))
    }

    override fun drop(from: Int, to: Int) {
        if (from == to) return
        cancelSelection()

        val habitFrom = adapter.getItem(from)
        val habitTo = adapter.getItem(to)
        if(habitFrom == null || habitTo == null) return

        adapter.performReorder(from, to)
        behavior.onReorderHabit(habitFrom, habitTo)
    }

    override fun onItemClick(position: Int) {
        activeMode.onItemClick(position)
    }

    override fun onItemLongClick(position: Int) {
        activeMode.onItemLongClick(position)
    }

    override fun onModelChange() {
        if (adapter.isSelectionEmpty) {
            activeMode = NormalMode()
            selectionMenu.get().onSelectionFinish()
        }
    }

    fun onSelectionFinished() {
        cancelSelection()
    }

    override fun startDrag(position: Int) {
        activeMode.startDrag(position)
    }

    protected fun toggleSelection(position: Int) {
        adapter.toggleSelection(position)
        activeMode = if (adapter.isSelectionEmpty) NORMAL_MODE else SELECTION_MODE
    }

    private fun cancelSelection() {
        adapter.clearSelection()
        activeMode = NormalMode()
        selectionMenu.get().onSelectionFinish()
    }

    interface HabitListener {
        fun onHabitClick(habit: Habit)
        fun onHabitReorder(from: Habit, to: Habit)
    }

    /**
     * A Mode describes the behavior of the list upon clicking, long clicking
     * and dragging an item. This depends on whether some items are already
     * selected or not.
     */
    private interface Mode {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int): Boolean
        fun startDrag(position: Int)
    }

    /**
     * Mode activated when there are no items selected. Clicks trigger habit
     * click. Long clicks start selection.
     */
    internal inner class NormalMode : Mode {
        override fun onItemClick(position: Int) {
            val habit = adapter.getItem(position)
            if (habit == null) return
            behavior.onClickHabit(habit)
        }

        override fun onItemLongClick(position: Int): Boolean {
            startSelection(position)
            return true
        }

        override fun startDrag(position: Int) {
            startSelection(position)
        }

        protected fun startSelection(position: Int) {
            toggleSelection(position)
            activeMode = SELECTION_MODE
            selectionMenu.get().onSelectionStart()
        }
    }

    /**
     * Mode activated when some items are already selected. Clicks toggle
     * item selection. Long clicks select more items.
     */
    internal inner class SelectionMode : Mode {
        override fun onItemClick(position: Int) {
            toggleSelection(position)
            notifyListener()
        }

        override fun onItemLongClick(position: Int): Boolean {
            toggleSelection(position)
            notifyListener()
            return true
        }

        override fun startDrag(position: Int) {
            toggleSelection(position)
            notifyListener()
        }

        protected fun notifyListener() {
            if (activeMode === SELECTION_MODE)
                selectionMenu.get().onSelectionChange()
            else
                selectionMenu.get().onSelectionFinish()
        }
    }
}

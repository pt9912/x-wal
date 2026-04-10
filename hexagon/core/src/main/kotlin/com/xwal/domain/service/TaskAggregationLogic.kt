package com.xwal.domain.service

import com.xwal.domain.model.Task

/**
 * Pure domain logic for aggregating tasks from multiple engines.
 */
object TaskAggregationLogic {

    /**
     * Sorts tasks by creation date (newest first) and applies pagination.
     */
    fun sortAndPaginate(tasks: List<Task>, offset: Int, limit: Int): List<Task> {
        return tasks
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
    }
}

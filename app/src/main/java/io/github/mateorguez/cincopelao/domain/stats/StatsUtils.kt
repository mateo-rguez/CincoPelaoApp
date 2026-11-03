package io.github.mateorguez.cincopelao.domain.stats

import io.github.mateorguez.cincopelao.data.local.entity.Event

data class Stats(
    val currentWeighted: Float,
    val average: Float,
    val minScore: Float,
    val maxScore: Float,
    val eventCount: Int,
    val eventCountDone: Int,
    val eventCountLeft: Int,
    val eventCountTrunc: Int,
    val eventCountSusp: Int,
    val eventCountSufi: Int,
    val eventCountAcep: Int,
    val eventCountNotable: Int,
    val eventCountSobre: Int,
    val noPuntCount: Int,
    val noPuntDoneCount: Int,
    val finalScoring5: Float,
    val maxPotential: Float,
    val trend: Float,
    val eventCompletedPerc: Float,
    val scoreCompletedPerc: Float,
    val stdDeviation: Float,
    val median: Float,
    val range: Float
)

object StatsUtils {

    fun calculateStats(events: List<Event>): Stats {

        val scores: List<Float?> = events
            .filter { it.isPuntuable }
            .map { it.score }

        val weights: List<Float> = events
            .filter { it.isPuntuable }
            .map { it.weight!! }

        val minScores: List<Float?> = events
            .filter { it.isPuntuable }
            .map { it.minScore }


        val filtered = scores.filterNotNull()
        val eventCount = weights.size
        val eventCountDone = filtered.size
        val eventCountLeft = eventCount - eventCountDone
        val eventCountTrunc = scores.zip(minScores)
            .count { (score, minScore) ->
                score != null && minScore != null && score < minScore / 10
            }
        val eventCountSusp = scores.zip(minScores)
            .count { (score, minScore) ->
                score != null && score < 5 && (minScore == null || score >= minScore / 10)
            }
        val eventCountSufi = scores.zip(minScores)
            .count { (score, minScore) ->
                score != null && score >= 5 && score < 6 && (minScore == null || score >= minScore / 10)
            }
        val eventCountAcep = scores.zip(minScores)
            .count { (score, minScore) ->
                score != null && score >= 6 && score < 7 && (minScore == null || score >= minScore / 10)
            }
        val eventCountNotable = scores.zip(minScores)
            .count { (score, minScore) ->
                score != null && score >= 7 && score < 9 && (minScore == null || score >= minScore / 10)
            }
        val eventCountSobre = scores.zip(minScores)
            .count { (score, minScore) ->
                score != null && score >= 9 && (minScore == null || score >= minScore / 10)
            }
        val noPuntCount = events.count { !it.isPuntuable }
        val noPuntDoneCount = events.count { !it.isPuntuable && it.noPuntuableDone }
        val average = filtered.average().toFloat()
        val minScore = filtered.minOrNull() ?: 0f
        val maxScore = filtered.maxOrNull() ?: 0f

        val pairs = scores.zip(weights).zip(minScores) { (score, weight), minScore -> Triple(score, weight, minScore) }

        val currentWeighted = pairs.sumOf { (score, weight, minScore) ->
            if (score != null && score!! >= (minScore ?: 0f) /10) {
                (score * weight / 100f).toDouble()
            } else {
                0.0
            }
        }.toFloat()

        val finalScoring5 = pairs.sumOf { (score, weight, minScore) ->
            if ((score ?: 5f) >= (minScore ?: 0f) /10) {
                ((score ?: 5f) * weight / 100f).toDouble()
            } else {
                0.0
            }
        }.toFloat()

        val maxPotential = pairs.sumOf { (score, weight, minScore) ->
            if ((score ?: 10f) >= (minScore ?: 0f) /10) {
                ((score ?: 10f) * weight / 100f).toDouble()
            } else {
                0.0
            }
        }.toFloat()

        val eventCompletedPerc = (scores.count { it != null }.toFloat() / weights.size) * 100

        val scoreCompletedPerc = scores.zip(weights)
            .sumOf { (score, weight) -> if (score != null) weight.toDouble() else 0.0 }.toFloat()

        val trend = calculateTrend(scores)

        val median = median(scores)

        val range = maxScore - minScore

        val stdDeviation = stdDev(filtered)
        return Stats(currentWeighted, average, minScore, maxScore, eventCount, eventCountDone, eventCountLeft, eventCountTrunc, eventCountSusp, eventCountSufi, eventCountAcep, eventCountNotable, eventCountSobre, noPuntCount, noPuntDoneCount, finalScoring5, maxPotential, trend, eventCompletedPerc, scoreCompletedPerc, stdDeviation, median, range)
    }

    fun stdDev(scores: List<Float?>): Float {
        val mean = scores.mapNotNull { it }.average()
        return kotlin.math.sqrt(scores.map { (it!! - mean) * (it!! - mean) }.average()).toFloat()
    }

    fun median(scores: List<Float?>): Float {
        val sorted = scores.filterNotNull().sorted()
        val size = sorted.size

        return when {
            size == 0 -> 0f
            size % 2 == 1 -> sorted[size / 2]
            else -> (sorted[size / 2 - 1] + sorted[size / 2]) / 2
        }
    }

    fun calculateTrend(scores: List<Float?>): Float {
        val filtered = scores.mapIndexedNotNull { index, score ->
            score?.let { index.toFloat() to it }
        }

        if (filtered.size < 2) return 0f

        val n = filtered.size
        val sumX = filtered.sumOf { it.first.toDouble() }.toFloat()
        val sumY = filtered.sumOf { it.second.toDouble() }.toFloat()
        val sumXY = filtered.sumOf { it.first.toDouble() * it.second.toDouble() }.toFloat()
        val sumX2 = filtered.sumOf { it.first.toDouble() * it.first.toDouble() }.toFloat()

        val numerator = n * sumXY - sumX * sumY
        val denominator = n * sumX2 - sumX * sumX

        return if (denominator != 0f) numerator / denominator else 0f
    }

}
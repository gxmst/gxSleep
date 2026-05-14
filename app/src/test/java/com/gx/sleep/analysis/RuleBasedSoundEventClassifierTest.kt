package com.gx.sleep.analysis

import com.gx.sleep.domain.model.SoundEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleBasedSoundEventClassifierTest {

    private lateinit var classifier: RuleBasedSoundEventClassifier

    @Before
    fun setup() {
        classifier = RuleBasedSoundEventClassifier()
    }

    @Test
    fun `classify IMPACT_NOISE when very short and very loud`() {
        // duration < 500ms and maxDbfs > -20dB
        val (type, confidence) = classifier.classify(
            durationMs = 200,
            avgRms = 5000f,
            maxRms = 8000f,
            avgZcr = 0.1f,
            avgDbfs = -15f,
            maxDbfs = -10f,
            lowBandRatio = 0.3f,
            midBandRatio = 0.4f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.IMPACT_NOISE, type)
        assertEquals(0.75f, confidence, 0.01f)
    }

    @Test
    fun `classify IMPACT_NOISE with exact boundary values`() {
        // Exactly at boundary: duration = 499ms, maxDbfs = -20dB
        val (type, _) = classifier.classify(
            durationMs = 499,
            avgRms = 5000f,
            maxRms = 8000f,
            avgZcr = 0.1f,
            avgDbfs = -20f,
            maxDbfs = -20f,
            lowBandRatio = 0.3f,
            midBandRatio = 0.4f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.IMPACT_NOISE, type)
    }

    @Test
    fun `not classify IMPACT_NOISE when duration too long`() {
        // duration = 500ms should not be IMPACT_NOISE
        val (type, _) = classifier.classify(
            durationMs = 500,
            avgRms = 5000f,
            maxRms = 8000f,
            avgZcr = 0.1f,
            avgDbfs = -15f,
            maxDbfs = -10f,
            lowBandRatio = 0.3f,
            midBandRatio = 0.4f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.IMPACT_NOISE)
    }

    @Test
    fun `not classify IMPACT_NOISE when not loud enough`() {
        // maxDbfs = -21dB should not be IMPACT_NOISE
        val (type, _) = classifier.classify(
            durationMs = 200,
            avgRms = 5000f,
            maxRms = 8000f,
            avgZcr = 0.1f,
            avgDbfs = -25f,
            maxDbfs = -21f,
            lowBandRatio = 0.3f,
            midBandRatio = 0.4f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.IMPACT_NOISE)
    }

    @Test
    fun `classify COUGH_LIKE when short burst with high frequency`() {
        // duration 100-1200ms, maxDbfs > -25dB, highBandRatio > 0.2
        val (type, confidence) = classifier.classify(
            durationMs = 500,
            avgRms = 4000f,
            maxRms = 6000f,
            avgZcr = 0.12f,
            avgDbfs = -20f,
            maxDbfs = -15f,
            lowBandRatio = 0.2f,
            midBandRatio = 0.3f,
            highBandRatio = 0.25f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.COUGH_LIKE, type)
        assertEquals(0.65f, confidence, 0.01f)
    }

    @Test
    fun `classify COUGH_LIKE with exact boundary values`() {
        // Exactly at boundary: duration = 100ms, maxDbfs = -25dB, highBandRatio = 0.2
        val (type, _) = classifier.classify(
            durationMs = 100,
            avgRms = 4000f,
            maxRms = 6000f,
            avgZcr = 0.12f,
            avgDbfs = -25f,
            maxDbfs = -25f,
            lowBandRatio = 0.2f,
            midBandRatio = 0.3f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.COUGH_LIKE, type)
    }

    @Test
    fun `not classify COUGH_LIKE when duration too short`() {
        // duration = 99ms should not be COUGH_LIKE
        val (type, _) = classifier.classify(
            durationMs = 99,
            avgRms = 4000f,
            maxRms = 6000f,
            avgZcr = 0.12f,
            avgDbfs = -20f,
            maxDbfs = -15f,
            lowBandRatio = 0.2f,
            midBandRatio = 0.3f,
            highBandRatio = 0.25f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.COUGH_LIKE)
    }

    @Test
    fun `not classify COUGH_LIKE when high frequency too low`() {
        // highBandRatio = 0.19 should not be COUGH_LIKE
        val (type, _) = classifier.classify(
            durationMs = 500,
            avgRms = 4000f,
            maxRms = 6000f,
            avgZcr = 0.12f,
            avgDbfs = -20f,
            maxDbfs = -15f,
            lowBandRatio = 0.2f,
            midBandRatio = 0.3f,
            highBandRatio = 0.19f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.COUGH_LIKE)
    }

    @Test
    fun `classify SNORE_LIKE when long, low frequency, periodic`() {
        // duration > 500ms, lowBandRatio > 0.4, avgZcr < 0.15, roughness > 0.3
        val (type, confidence) = classifier.classify(
            durationMs = 1500,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.1f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.5f,
            midBandRatio = 0.3f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.4f
        )
        assertEquals(SoundEventType.SNORE_LIKE, type)
        assertEquals(0.65f, confidence, 0.01f)
    }

    @Test
    fun `classify SNORE_LIKE with high confidence when very long and low frequency`() {
        // duration > 2000ms, lowBandRatio > 0.6
        val (type, confidence) = classifier.classify(
            durationMs = 2500,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.1f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.7f,
            midBandRatio = 0.2f,
            highBandRatio = 0.1f,
            rmsEnvelopeRoughness = 0.5f
        )
        assertEquals(SoundEventType.SNORE_LIKE, type)
        assertEquals(0.8f, confidence, 0.01f)
    }

    @Test
    fun `classify SNORE_LIKE with medium confidence when medium duration`() {
        // duration 1000-2000ms
        val (type, confidence) = classifier.classify(
            durationMs = 1200,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.1f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.5f,
            midBandRatio = 0.3f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.4f
        )
        assertEquals(SoundEventType.SNORE_LIKE, type)
        assertEquals(0.65f, confidence, 0.01f)
    }

    @Test
    fun `classify SNORE_LIKE with low confidence when short duration`() {
        // duration 500-1000ms
        val (type, confidence) = classifier.classify(
            durationMs = 800,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.1f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.5f,
            midBandRatio = 0.3f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.4f
        )
        assertEquals(SoundEventType.SNORE_LIKE, type)
        assertEquals(0.5f, confidence, 0.01f)
    }

    @Test
    fun `not classify SNORE_LIKE when ZCR too high`() {
        // avgZcr = 0.15 should not be SNORE_LIKE
        val (type, _) = classifier.classify(
            durationMs = 1500,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.15f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.5f,
            midBandRatio = 0.3f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.4f
        )
        assertTrue(type != SoundEventType.SNORE_LIKE)
    }

    @Test
    fun `not classify SNORE_LIKE when roughness too low`() {
        // roughness = 0.3 should not be SNORE_LIKE
        val (type, _) = classifier.classify(
            durationMs = 1500,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.1f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.5f,
            midBandRatio = 0.3f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.3f
        )
        assertTrue(type != SoundEventType.SNORE_LIKE)
    }

    @Test
    fun `classify SPEECH_LIKE when medium duration and higher ZCR`() {
        // duration 300-5000ms, avgZcr > 0.08, mid or high frequency
        val (type, confidence) = classifier.classify(
            durationMs = 1000,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.12f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.3f,
            midBandRatio = 0.25f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.SPEECH_LIKE, type)
        assertEquals(0.6f, confidence, 0.01f)
    }

    @Test
    fun `classify SPEECH_LIKE with exact boundary values`() {
        // Exactly at boundary: duration = 300ms, avgZcr = 0.08, midBandRatio = 0.2
        val (type, _) = classifier.classify(
            durationMs = 300,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.08f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.3f,
            midBandRatio = 0.2f,
            highBandRatio = 0.1f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.SPEECH_LIKE, type)
    }

    @Test
    fun `not classify SPEECH_LIKE when duration too short`() {
        // duration = 299ms should not be SPEECH_LIKE
        val (type, _) = classifier.classify(
            durationMs = 299,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.12f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.3f,
            midBandRatio = 0.25f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.SPEECH_LIKE)
    }

    @Test
    fun `not classify SPEECH_LIKE when ZCR too low`() {
        // avgZcr = 0.079 should not be SPEECH_LIKE
        val (type, _) = classifier.classify(
            durationMs = 1000,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.079f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.3f,
            midBandRatio = 0.25f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.SPEECH_LIKE)
    }

    @Test
    fun `classify MOVEMENT_FRICTION when medium duration and quiet with wide spectrum`() {
        // duration 1000-3000ms, avgDbfs -25 to -45, ZCR 0.06-0.12, balanced bands
        val (type, confidence) = classifier.classify(
            durationMs = 1500,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.09f,
            avgDbfs = -35f,
            maxDbfs = -28f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.MOVEMENT_FRICTION, type)
        assertEquals(0.65f, confidence, 0.01f)
    }

    @Test
    fun `classify MOVEMENT_FRICTION with exact boundary values`() {
        // Exactly at boundary: duration = 1000ms, avgDbfs = -25dB, avgZcr = 0.06
        val (type, _) = classifier.classify(
            durationMs = 1000,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.06f,
            avgDbfs = -25f,
            maxDbfs = -20f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.MOVEMENT_FRICTION, type)
    }

    @Test
    fun `classify MOVEMENT_FRICTION with upper boundary values`() {
        // Exactly at boundary: duration = 3000ms, avgDbfs = -45dB, avgZcr = 0.12
        val (type, _) = classifier.classify(
            durationMs = 3000,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.12f,
            avgDbfs = -45f,
            maxDbfs = -38f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.MOVEMENT_FRICTION, type)
    }

    @Test
    fun `not classify MOVEMENT_FRICTION when duration too short`() {
        // duration = 999ms should not be MOVEMENT_FRICTION
        val (type, _) = classifier.classify(
            durationMs = 999,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.09f,
            avgDbfs = -35f,
            maxDbfs = -28f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.MOVEMENT_FRICTION)
    }

    @Test
    fun `not classify MOVEMENT_FRICTION when duration too long`() {
        // duration = 3001ms should not be MOVEMENT_FRICTION
        val (type, _) = classifier.classify(
            durationMs = 3001,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.09f,
            avgDbfs = -35f,
            maxDbfs = -28f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.MOVEMENT_FRICTION)
    }

    @Test
    fun `not classify MOVEMENT_FRICTION when too loud`() {
        // avgDbfs = -24dB should not be MOVEMENT_FRICTION
        val (type, _) = classifier.classify(
            durationMs = 1500,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.09f,
            avgDbfs = -24f,
            maxDbfs = -18f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.MOVEMENT_FRICTION)
    }

    @Test
    fun `not classify MOVEMENT_FRICTION when too quiet`() {
        // avgDbfs = -46dB should not be MOVEMENT_FRICTION
        val (type, _) = classifier.classify(
            durationMs = 1500,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.09f,
            avgDbfs = -46f,
            maxDbfs = -40f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.MOVEMENT_FRICTION)
    }

    @Test
    fun `not classify MOVEMENT_FRICTION when ZCR too low`() {
        // avgZcr = 0.059 should not be MOVEMENT_FRICTION
        val (type, _) = classifier.classify(
            durationMs = 1500,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.059f,
            avgDbfs = -35f,
            maxDbfs = -28f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.MOVEMENT_FRICTION)
    }

    @Test
    fun `not classify MOVEMENT_FRICTION when ZCR too high`() {
        // avgZcr = 0.121 should not be MOVEMENT_FRICTION
        val (type, _) = classifier.classify(
            durationMs = 1500,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.121f,
            avgDbfs = -35f,
            maxDbfs = -28f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.MOVEMENT_FRICTION)
    }

    @Test
    fun `not classify MOVEMENT_FRICTION when bands not balanced`() {
        // lowBandRatio too dominant, bandBalance < 0.3
        val (type, _) = classifier.classify(
            durationMs = 1500,
            avgRms = 800f,
            maxRms = 1500f,
            avgZcr = 0.09f,
            avgDbfs = -35f,
            maxDbfs = -28f,
            lowBandRatio = 0.6f,
            midBandRatio = 0.2f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertTrue(type != SoundEventType.MOVEMENT_FRICTION)
    }

    @Test
    fun `SPEECH_LIKE takes priority over MOVEMENT_FRICTION`() {
        // Both conditions could match: medium duration, some ZCR
        // SPEECH_LIKE is checked before MOVEMENT_FRICTION
        val (type, _) = classifier.classify(
            durationMs = 1500,
            avgRms = 2000f,
            maxRms = 3000f,
            avgZcr = 0.1f,
            avgDbfs = -30f,
            maxDbfs = -22f,
            lowBandRatio = 0.3f,
            midBandRatio = 0.25f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.SPEECH_LIKE, type)
    }

    @Test
    fun `classify ENVIRONMENT_NOISE when long and quiet`() {
        // duration > 3000ms, avgDbfs < -30dB
        val (type, confidence) = classifier.classify(
            durationMs = 5000,
            avgRms = 1000f,
            maxRms = 2000f,
            avgZcr = 0.05f,
            avgDbfs = -35f,
            maxDbfs = -30f,
            lowBandRatio = 0.4f,
            midBandRatio = 0.3f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.1f
        )
        assertEquals(SoundEventType.ENVIRONMENT_NOISE, type)
        assertEquals(0.7f, confidence, 0.01f)
    }

    @Test
    fun `classify ENVIRONMENT_NOISE with exact boundary values`() {
        // Exactly at boundary: duration = 3001ms, avgDbfs = -30dB
        val (type, _) = classifier.classify(
            durationMs = 3001,
            avgRms = 1000f,
            maxRms = 2000f,
            avgZcr = 0.05f,
            avgDbfs = -30f,
            maxDbfs = -30f,
            lowBandRatio = 0.4f,
            midBandRatio = 0.3f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.1f
        )
        assertEquals(SoundEventType.ENVIRONMENT_NOISE, type)
    }

    @Test
    fun `not classify ENVIRONMENT_NOISE when too short`() {
        // duration = 3000ms should not be ENVIRONMENT_NOISE
        val (type, _) = classifier.classify(
            durationMs = 3000,
            avgRms = 1000f,
            maxRms = 2000f,
            avgZcr = 0.05f,
            avgDbfs = -35f,
            maxDbfs = -30f,
            lowBandRatio = 0.4f,
            midBandRatio = 0.3f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.1f
        )
        assertTrue(type != SoundEventType.ENVIRONMENT_NOISE)
    }

    @Test
    fun `not classify ENVIRONMENT_NOISE when too loud`() {
        // avgDbfs = -29dB should not be ENVIRONMENT_NOISE
        val (type, _) = classifier.classify(
            durationMs = 5000,
            avgRms = 1000f,
            maxRms = 2000f,
            avgZcr = 0.05f,
            avgDbfs = -29f,
            maxDbfs = -30f,
            lowBandRatio = 0.4f,
            midBandRatio = 0.3f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.1f
        )
        assertTrue(type != SoundEventType.ENVIRONMENT_NOISE)
    }

    @Test
    fun `classify UNKNOWN when no pattern matches`() {
        // Medium duration, medium loudness, no distinctive features
        val (type, confidence) = classifier.classify(
            durationMs = 1000,
            avgRms = 2000f,
            maxRms = 3000f,
            avgZcr = 0.06f,
            avgDbfs = -28f,
            maxDbfs = -22f,
            lowBandRatio = 0.35f,
            midBandRatio = 0.35f,
            highBandRatio = 0.3f,
            rmsEnvelopeRoughness = 0.25f
        )
        assertEquals(SoundEventType.UNKNOWN, type)
        assertEquals(0.3f, confidence, 0.01f)
    }

    @Test
    fun `IMPACT_NOISE takes priority over COUGH_LIKE`() {
        // Both conditions could match: short and loud
        // IMPACT_NOISE is checked first
        val (type, _) = classifier.classify(
            durationMs = 200,
            avgRms = 5000f,
            maxRms = 8000f,
            avgZcr = 0.12f,
            avgDbfs = -15f,
            maxDbfs = -10f,
            lowBandRatio = 0.2f,
            midBandRatio = 0.3f,
            highBandRatio = 0.25f,
            rmsEnvelopeRoughness = 0.2f
        )
        assertEquals(SoundEventType.IMPACT_NOISE, type)
    }

    @Test
    fun `COUGH_LIKE takes priority over SNORE_LIKE`() {
        // Both conditions could match: medium duration, some low frequency
        // COUGH_LIKE is checked before SNORE_LIKE
        val (type, _) = classifier.classify(
            durationMs = 800,
            avgRms = 4000f,
            maxRms = 6000f,
            avgZcr = 0.1f,
            avgDbfs = -20f,
            maxDbfs = -15f,
            lowBandRatio = 0.5f,
            midBandRatio = 0.3f,
            highBandRatio = 0.25f,
            rmsEnvelopeRoughness = 0.4f
        )
        assertEquals(SoundEventType.COUGH_LIKE, type)
    }

    @Test
    fun `SNORE_LIKE takes priority over SPEECH_LIKE`() {
        // Both conditions could match: medium duration, some ZCR
        // SNORE_LIKE is checked before SPEECH_LIKE
        val (type, _) = classifier.classify(
            durationMs = 1500,
            avgRms = 3000f,
            maxRms = 5000f,
            avgZcr = 0.1f,
            avgDbfs = -25f,
            maxDbfs = -15f,
            lowBandRatio = 0.5f,
            midBandRatio = 0.3f,
            highBandRatio = 0.2f,
            rmsEnvelopeRoughness = 0.4f
        )
        assertEquals(SoundEventType.SNORE_LIKE, type)
    }
}
package com.animalvision.core

import com.animalvision.core.model.ProfileValidator
import com.animalvision.core.profiles.profileRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Profile integrity + scientific-paranoia tests (spec §40 "Scientific validation").
 * These pin the documented research values so a profile can never silently drift from
 * the literature (docs/SCIENCE.md).
 */
class ProfilesTest {

    @Test
    fun `registry contains exactly the eight supported profiles in order`() {
        assertThat(profileRegistry.map { it.id }).containsExactly(
            "human", "dog", "cat", "horse", "eagle", "bee", "snake",
        ).inOrder()
    }

    @Test
    fun `every profile passes full validation`() {
        for (profile in profileRegistry) {
            val report = ProfileValidator.validate(profile)
            assertThat(report.issues).isEmpty()
        }
    }

    @Test
    fun `every profile is independently versioned and dated`() {
        for (profile in profileRegistry) {
            assertThat(profile.profileVersion).matches("""\d+\.\d+\.\d+""")
            assertThat(profile.researchUpdated).matches("""\d{4}-\d{2}-\d{2}""")
        }
    }

    @Test
    fun `dog cone peaks are 429 and 555 nm - measured by Neitz 1989`() {
        val dog = com.animalvision.core.profiles.getProfile("dog")!!
        assertThat(dog.colorVision.sensitivities.map { it.peakNm }).containsExactly(429.0, 555.0)
        assertThat(dog.colorVision.photoreceptorCount).isEqualTo(2)
    }

    @Test
    fun `horse cone peaks are 428 and 539 nm - measured by Carroll 2001`() {
        val horse = com.animalvision.core.profiles.getProfile("horse")!!
        assertThat(horse.colorVision.sensitivities.map { it.peakNm }).containsExactly(428.0, 539.0)
        assertThat(horse.fieldOfView.totalHorizontalDeg).isEqualTo(350.0)
        assertThat(horse.fieldOfView.binocularWidthDeg).isAtLeast(55.0)
        assertThat(horse.fieldOfView.binocularWidthDeg).isAtMost(65.0)
        assertThat(horse.fieldOfView.blindSpotLabels).isNotEmpty()
    }

    @Test
    fun `bee photoreceptor peaks are 344, 436 and 556 nm - measured by Peitsch 1992`() {
        val bee = com.animalvision.core.profiles.getProfile("bee")!!
        assertThat(bee.colorVision.sensitivities.map { it.peakNm }).containsExactly(344.0, 436.0, 556.0)
        assertThat(bee.acuity.cutoffCpd).isAtMost(1.0)
        assertThat(bee.specialVision.ultraviolet!!.isSimulation).isTrue()
    }

    @Test
    fun `cat cone peaks are 452 and 556 nm - measured molecularly`() {
        val cat = com.animalvision.core.profiles.getProfile("cat")!!
        assertThat(cat.colorVision.sensitivities.map { it.peakNm }).containsExactly(452.0, 556.0)
        assertThat(cat.lowLight.sensitivity).isAtLeast(5.0) // ~1/6 human light requirement
    }

    @Test
    fun `eagle acuity is 140 cpd - behavioral limit measured by Reymond 1985`() {
        val eagle = com.animalvision.core.profiles.getProfile("eagle")!!
        assertThat(eagle.acuity.cutoffCpd).isEqualTo(140.0)
        assertThat(eagle.acuity.centralScalar).isGreaterThan(1.0) // foveal boost zone
        assertThat(eagle.specialVision.ultraviolet).isNull() // no UV claim - Lind et al. 2013
    }

    @Test
    fun `snake thermal simulation is labeled as hardware-dependent`() {
        val snake = com.animalvision.core.profiles.getProfile("snake")!!
        val ir = snake.specialVision.infrared
        assertThat(ir).isNotNull()
        assertThat(ir!!.speciesHasPitOrgan).isTrue()
        assertThat(ir.needsHardware).isTrue()
        assertThat(ir.label).contains("SIMULATION")
    }

    @Test
    fun `every profile declares sources and limitations`() {
        for (profile in profileRegistry) {
            assertThat(profile.scientificData.sources).isNotEmpty()
            assertThat(profile.scientificData.limitations).isNotEmpty()
        }
    }

    @Test
    fun `unknown profile id throws and null-safe lookup returns null`() {
        assertThat(com.animalvision.core.profiles.getProfile("unicorn")).isNull()
        var threw = false
        try {
            com.animalvision.core.profiles.requireProfile("unicorn")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }
}

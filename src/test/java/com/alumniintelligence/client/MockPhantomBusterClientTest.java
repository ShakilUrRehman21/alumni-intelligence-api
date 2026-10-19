package com.alumniintelligence.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link MockPhantomBusterClient}.
 *
 * <p>Verifies that the stub returns the expected fixture profiles and
 * handles parameter variations correctly.
 */
class MockPhantomBusterClientTest {

    private final MockPhantomBusterClient client = new MockPhantomBusterClient();

    @Test
    @DisplayName("Returns 3 fixture profiles")
    void searchAlumni_returnsThreeProfiles() {
        List<PhantomBusterProfile> profiles =
                client.searchAlumni("MIT", "Engineer", 2020);

        assertThat(profiles).hasSize(3);
    }

    @Test
    @DisplayName("All profiles have non-null fullName")
    void searchAlumni_profilesHaveNames() {
        List<PhantomBusterProfile> profiles =
                client.searchAlumni("Harvard", "Scientist", null);

        assertThat(profiles).extracting(PhantomBusterProfile::fullName)
                .doesNotContainNull();
    }

    @Test
    @DisplayName("When passoutYear provided, it is reflected in all fixture profiles")
    void searchAlumni_withPassoutYear_usesProvidedYear() {
        List<PhantomBusterProfile> profiles =
                client.searchAlumni("Stanford", "Manager", 2018);

        assertThat(profiles)
                .extracting(PhantomBusterProfile::graduationYear)
                .containsOnly(2018);
    }

    @Test
    @DisplayName("When passoutYear is null, fixture profiles use default years")
    void searchAlumni_withoutPassoutYear_usesDefaultYears() {
        List<PhantomBusterProfile> profiles =
                client.searchAlumni("Oxford", "Developer", null);

        // Each fixture has its own hardcoded default year
        assertThat(profiles)
                .extracting(PhantomBusterProfile::graduationYear)
                .doesNotContainNull();
    }

    @Test
    @DisplayName("Profile URLs are unique and non-null")
    void searchAlumni_profileUrlsAreUniqueAndNonNull() {
        List<PhantomBusterProfile> profiles =
                client.searchAlumni("Cambridge", "Analyst", null);

        assertThat(profiles)
                .extracting(PhantomBusterProfile::profileUrl)
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("University name is set correctly")
    void searchAlumni_universitySetFromParameter() {
        List<PhantomBusterProfile> profiles =
                client.searchAlumni("Princeton", "Researcher", null);

        assertThat(profiles)
                .extracting(PhantomBusterProfile::school)
                .containsOnly("Princeton");
    }
}

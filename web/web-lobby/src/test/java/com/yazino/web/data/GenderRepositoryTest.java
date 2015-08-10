package com.yazino.web.data;

import com.yazino.platform.player.Gender;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class GenderRepositoryTest {

    private GenderRepository genderRepository = new GenderRepository();

    @Test
    public void gendersContainsMale() {
        assertThat(genderRepository.getGenders().containsKey(Gender.MALE.getId()), is(true));
        assertThat(genderRepository.getGenders().get(Gender.MALE.getId()), is(equalTo(Gender.MALE.getName())));
    }

    @Test
    public void gendersContainsFemale() {
        assertThat(genderRepository.getGenders().containsKey(Gender.FEMALE.getId()), is(true));
        assertThat(genderRepository.getGenders().get(Gender.FEMALE.getId()), is(equalTo(Gender.FEMALE.getName())));
    }

    @Test
    public void gendersContainsOnlyTwoGenders() {
        assertThat(genderRepository.getGenders().size(), is(equalTo(2)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void gendersIsNotModifiable() {
        genderRepository.getGenders().put("aGender", "aName");
    }

}

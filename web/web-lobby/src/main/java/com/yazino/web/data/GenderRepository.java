package com.yazino.web.data;

import com.yazino.platform.player.Gender;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class GenderRepository {

    private final Map<String, String> genders;

    public GenderRepository() {
        genders = buildGenders();
    }

    public Map<String, String> getGenders() {
        return Collections.unmodifiableMap(genders);
    }

    private Map<String, String> buildGenders() {
        final Map<String, String> gendersMap = new LinkedHashMap<String, String>();
        gendersMap.put(Gender.MALE.getId(), Gender.MALE.getName());
        gendersMap.put(Gender.FEMALE.getId(), Gender.FEMALE.getName());
        return gendersMap;
    }

}

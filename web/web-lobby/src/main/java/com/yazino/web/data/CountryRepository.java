package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.reference.Country;
import com.yazino.platform.reference.ReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class CountryRepository {

    private final ReferenceService referenceService;

    CountryRepository() {
        // cglib constructor
        referenceService = null;
    }

    @Autowired
    public CountryRepository(final ReferenceService referenceService) {
        notNull(referenceService, "referenceService may not be null");

        this.referenceService = referenceService;
    }

    private void verifyInitialised() {
        if (referenceService == null) {
            throw new IllegalStateException("This class was created with the CGLib constructor");
        }
    }

    @Cacheable(cacheName = "countryRepositoryCache")
    public Map<String, String> getCountries() {
        verifyInitialised();

        final Map<String, String> countriesByCode = new LinkedHashMap<String, String>();
        for (Country country : sortedCountries()) {
            countriesByCode.put(country.getIso3166CountryCode(), country.getName());
        }
        return countriesByCode;
    }

    private List<Country> sortedCountries() {
        final Set<Country> countrySet = referenceService.getCountries();
        if (countrySet == null) {
            return Collections.emptyList();
        }

        final ArrayList<Country> countries = new ArrayList<Country>(countrySet);
        Collections.sort(countries);
        return countries;
    }
}

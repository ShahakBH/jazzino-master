package com.yazino.web.controller.social;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

public class TestHelper {

    private static final Log LOG = LogFactory.getLog(TestHelper.class);

    public static boolean classesMatch(String actualClassString, String ... expected) {
        String[] allActualClassesTmp = actualClassString.split(" ");
        ArrayList<String> allActualClasses = new ArrayList<String>();
        for (int i = 0; i < allActualClassesTmp.length; i++) {
            allActualClasses.add(allActualClassesTmp[i]);
        }
        for (int i = 0; i < expected.length; i++) {
            if (allActualClasses.indexOf(expected[i]) == -1) {
                LOG.warn("There is no class [{" + expected[i] + "}] in classString [{" + actualClassString + "}]");
                return false;
            }
        }
        if (allActualClasses.size() == expected.length) {
            return true;
        } else {
            LOG.warn("Number of items don't match expected [" + expected.toString() + "] got [" + actualClassString + "]");
            return false;
        }
    }

}

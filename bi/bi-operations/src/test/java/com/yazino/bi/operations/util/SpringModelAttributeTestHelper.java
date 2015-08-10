package com.yazino.bi.operations.util;

import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.yazino.bi.operations.controller.PromotionController;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/*
    Useful to verify the wiring of Spring MVC @ModelAttribute methods
 */
public class SpringModelAttributeTestHelper {

    public static Model collectModelAttributes(PromotionController controller) {
        Model model = new ExtendedModelMap();
        Set<Method> modelAttributeContributors = findModelAttributeContributors(controller);
        for (Method contributor : modelAttributeContributors) {
            collectModelAttribute(model, controller, contributor);
        }
        return model;
    }

    private static void collectModelAttribute(Model model, PromotionController controller, Method contributor) {
        try {
            ModelAttribute modelAttribute = contributor.getAnnotation(ModelAttribute.class);
            if (modelAttribute != null) {
                String attributeName = modelAttribute.value();
                Object attributeValue = null;
                attributeValue = contributor.invoke(controller);
                model.addAttribute(attributeName, attributeValue);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke model attribute contributor " + contributor.getName(), e);
        }
    }

    private static Set<Method> findModelAttributeContributors(Object controller) {
        Set<Method> contributors = new HashSet<Method>();
        for (Method method : controller.getClass().getMethods()) {
            ModelAttribute modelAttribute = method.getAnnotation(ModelAttribute.class);
            if (modelAttribute != null) {
                contributors.add(method);
            }
        }
        return contributors;
    }
}

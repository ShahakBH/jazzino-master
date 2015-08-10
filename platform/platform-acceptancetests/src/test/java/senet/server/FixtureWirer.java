package senet.server;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.neuri.trinidad.transactionalrunner.FitnesseSpringContext;

import fit.Fixture;

public class FixtureWirer {
	private static final AutowireCapableBeanFactory beanFactory;

    static {
		beanFactory = getAutowireCapableBeanFactory();
    }

	private static AutowireCapableBeanFactory getAutowireCapableBeanFactory() {
		try {
			System.out.println("Using FitnesseSpringContext");
			return  FitnesseSpringContext.getInstance().getAutowireCapableBeanFactory();
		}
		catch (Throwable t){
			System.out.println("Using ClassPathXmlApplicationContext");
        	return  new ClassPathXmlApplicationContext("/spring.xml").getAutowireCapableBeanFactory();
		}

	}
    public static void wire(Fixture fixture) {

        // make sure you AUTOWIRE_BY_NAME otherwise Spring will complain
        // about injecting the systemUnderTest property in the superclass which is of type object.
        beanFactory.autowireBeanProperties(fixture, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
    }

}

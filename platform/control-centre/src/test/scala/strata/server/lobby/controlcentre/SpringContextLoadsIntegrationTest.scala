package strata.server.lobby.controlcentre

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class SpringContextLoadsIntegrationTest extends FlatSpec with ShouldMatchers {

    "The Spring Context" should "load without error" in {
        val context = new ClassPathXmlApplicationContext("classpath:/META-INF/spring/root-handler-spring.xml");

        context.getBean("velocityViewResolver") should not be (null)
    }

}

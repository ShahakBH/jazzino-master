package strata.server.lobby.controlcentre.util


import java.util.{Iterator => JavaIterator}
import org.apache.velocity.util.introspection.UberspectImpl.VelGetterImpl
import org.apache.velocity.runtime.log.Log
import org.apache.velocity.runtime.parser.node.{MapGetExecutor, PropertyExecutor}
import org.apache.velocity.util.introspection.{Introspector, VelPropertyGet, Info, UberspectImpl}

/*
* Improved Velocity integration for Scala.
* From Scott McSweeney-Roberts (http://www.frothandjava.com/2010/02/scala-spring-templates-velocity.html)
*/
class ScalaVelocityUberspect extends UberspectImpl {

    override def getIterator(obj: java.lang.Object, i: Info): JavaIterator[_] = {
        def makeJavaIterator(iter: Iterator[_]): JavaIterator[AnyRef] = new JavaIterator[AnyRef] {
            override def hasNext: Boolean = iter.hasNext

            override def next(): AnyRef = iter.next().asInstanceOf[AnyRef]

            override def remove() {
                throw new java.lang.UnsupportedOperationException("Remove not supported")
            }
        }

        obj match {
            case i: Iterable[_] => makeJavaIterator(i.iterator)
            case i: Iterator[_] => makeJavaIterator(i)
            case _ => super.getIterator(obj, i)
        }
    }

    override def getPropertyGet(obj: java.lang.Object, identifier: String, i: Info): VelPropertyGet = {
        if (obj != null) {
            val claz = obj.getClass

            val executor = obj match {
                case m: Map[_, _] => new ScalaMapGetExecutor(log, claz, identifier)
                case _ => new ScalaPropertyExecutor(log, introspector, claz, identifier)

            }

            if (executor.isAlive) {
                new VelGetterImpl(executor)
            } else {
                super.getPropertyGet(obj, identifier, i)
            }
        } else {
            null
        }
    }

}

class ScalaPropertyExecutor(val llog: Log, val introspector: Introspector, val clazz: java.lang.Class[_], val property: String)
        extends PropertyExecutor(llog, introspector, clazz, property) {

    override def discover(clazz: java.lang.Class[_], property: String) {
        val params = Array[java.lang.Object]()
        setMethod(getIntrospector.getMethod(clazz, property, params))
        if (!isAlive) {
            super.discover(clazz, property)
        }
    }
}

class ScalaMapGetExecutor(val llog: Log, val clazz: java.lang.Class[_], val property: String) extends MapGetExecutor(llog, clazz, property) {
    override def isAlive: Boolean = true

    override def execute(o: AnyRef): java.lang.Object = o.asInstanceOf[Map[String, AnyRef]]
            .getOrElse[AnyRef](property, null).asInstanceOf[java.lang.Object]

}

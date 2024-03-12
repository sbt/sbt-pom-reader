import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers._

class MainTest extends AnyFunSuite with Matchers {

  test("hi shouldbe hi") {
    Main.hi should be("hi")
  }

}


import org.scalatest._

class MainTest extends FunSuite with Matchers {


  test("hi shouldbe hi") {
    Main.hi should be ("hi")
  }

}
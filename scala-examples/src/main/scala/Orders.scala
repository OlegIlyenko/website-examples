import language.postfixOps

import com.ning.http.client.Realm
import dispatch._, Defaults._
import scala.concurrent.{Awaitable, Await}
import scala.concurrent.duration.Duration
import net.liftweb.json._
import net.liftweb.json.JsonDSL._


object Orders extends App {

  val CLIENT_ID = ""
  val CLIENT_SECRET = ""

  val authToken  = host("auth.sphere.io").secure / "oauth" / "token"

  val tokenReq = authToken.POST
    .addParameter("grant_type", "client_credentials")
    .addParameter("scope", "manage_project:my-project-key")
    .setRealm(new Realm.RealmBuilder()
      .setPrincipal(CLIENT_ID)
      .setPassword(CLIENT_SECRET)
      .setUsePreemptiveAuth(true)
      .setScheme(Realm.AuthScheme.BASIC).build)

  val JString(token) = Http(tokenReq OK as.lift.Json).apply \ "access_token"

  println(token)

  // Example for Almost everything section: START!!!!

  val productSearch = host("api.sphere.io").secure / "my-project-key" /
    "product-projections" / "search"

  val snowboardsReq = productSearch
    .addHeader("Authorization", "Bearer " + token)
    .addQueryParameter("lang", "en")
    .addQueryParameter("text", "snowboard")
    .addQueryParameter("filter", "variants.price.centAmount:range(20000 to 40000)")

  val snowboards = Http(snowboardsReq OK as.lift.Json)

  // Example for Almost everything section: END!!!!

  println(snowboards())

  var cartVersion: JValue = 0

  val JString(cartId) = {
    // Example "Create Cart": START!!!

    val carts = host("api.sphere.io").secure / "my-project-key" / "carts"

    val createCartReq = carts.POST
      .addHeader("Authorization", "Bearer " + token)
      .setBody(compact(render(
        ("currency" -> "EUR") ~
        ("country" -> "DE") ~
        ("inventoryMode" -> "ReserveOnOrder")
      )))

    val cart = Http(createCartReq OK as.lift.Json)

    // Example "Create Cart": END!!!

    cartVersion = cart() \ "version"
    cart() \ "id"
  }

  println(cartId)
  println(cartVersion)

  {
    // Example "Set Shipping Address": START!!!

    val cartEndpoint = host("api.sphere.io").secure / "my-project-key" / "carts" / cartId

    val setShippingAddressReq = cartEndpoint.POST
      .addHeader("Authorization", "Bearer " + token)
      .setBody(compact(render(
        ("version" -> cartVersion) ~
        ("actions" -> List(
          ("action" -> "setShippingAddress") ~
          ("address" -> (
            ("country" -> "DE") ~
            ("city" -> "Berlin") ~
            ("streetName" -> "Friedrichstraße") ~
            ("streetNumber" -> "1")
          ))
        ))
      )))

    val cart = Http(setShippingAddressReq OK as.lift.Json)

    // Example "Set Shipping Address": END!!!

    cartVersion = cart() \ "version"
  }

  println(cartVersion)

  {
    val productId: String = "c8c6f130-9bca-4263-b6b6-abcd4cdde1b9"

    // Example "Add Line Item to Cart": START!!!

    val cartEndpoint = host("api.sphere.io").secure / "my-project-key" / "carts" / cartId

    val addLineItemReq = cartEndpoint.POST
      .addHeader("Authorization", "Bearer " + token)
      .setBody(compact(render(
      ("version" -> cartVersion) ~
        ("actions" -> List(
          ("action" -> "addLineItem") ~
          ("productId" -> productId) ~
          ("variantId" -> 1) ~
          ("quantity" -> 15)
        ))
    )))

    val cart = Http(addLineItemReq OK as.lift.Json)

    // Example "Add Line Item to Cart": END!!!

    cartVersion = cart() \ "version"
  }

  println(cartVersion)

  {
    val productId: String = "c8c6f130-9bca-4263-b6b6-abcd4cdde1b9"

    // Example "Create Order from Cart": START!!!

    val orders = host("api.sphere.io").secure / "my-project-key" / "orders"

    val createOrderReq = orders.POST
      .addHeader("Authorization", "Bearer " + token)
      .setBody(compact(render(
        ("id" -> cartId) ~
        ("version" -> cartVersion) ~
        ("paymentState" -> "Paid")
      )))

    val order = Http(createOrderReq OK as.lift.Json)

    // Example "Create Order from Cart": END!!!

    cartVersion = order() \ "version"
  }

  println(cartVersion)

  sys.exit() // too lazy to stop async client properly
}
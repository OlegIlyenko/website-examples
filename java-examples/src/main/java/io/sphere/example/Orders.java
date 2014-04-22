package io.sphere.example;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentStringsMap;
import com.ning.http.client.Realm;
import com.ning.http.client.Response;

import java.util.Map;
import java.util.concurrent.Future;

public class Orders {
  public static final String CLIENT_ID = "";
  public static final String CLIENT_SECRET = "";

  public static void main(String[] args) throws Exception {
    AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    Gson gson = new Gson();

    Response tokenResp =
      asyncHttpClient
        .preparePost("https://auth.sphere.io/oauth/token")
        .setParameters(new FluentStringsMap().add("grant_type", "client_credentials").add("scope", "manage_project:my-project-key"))
        .setRealm(new Realm.RealmBuilder()
          .setPrincipal(CLIENT_ID)
          .setPassword(CLIENT_SECRET)
          .setUsePreemptiveAuth(true)
          .setScheme(Realm.AuthScheme.BASIC)
          .build())
        .execute()
        .get();

    System.out.println("Token JSON: " + tokenResp.getResponseBody());
    Map<String, Object> token = gson.fromJson(tokenResp.getResponseBody(), Map.class);


    // NOTE: We probably can skip the variable part when we copy/paste it to the website (this one: `Future<Response> snowboards = `)
    // Example for Almost everything section: START!!!!

    Future<Response> snowboards = asyncHttpClient
      .prepareGet("https://api.sphere.io/my-project-key/product-projections/search")
      .addHeader("Authorization", "Bearer " + token.get("access_token"))
      .addQueryParameter("lang", "en")
      .addQueryParameter("text", "snowboard")
      .addQueryParameter("filter", "variants.price.centAmount:range(20000 to 40000)")
      .execute();

    // Example for Almost everything section: END!!!!

    System.out.println(snowboards.get().getResponseBody());

    // Example "Create Cart": START!!!

    Future<Response> cart =
      asyncHttpClient
        .preparePost("https://api.sphere.io/my-project-key/carts")
        .addHeader("Authorization", "Bearer " + token.get("access_token"))
        .setBody(gson.toJson(ImmutableMap.of(
          "currency", "EUR",
          "country", "DE",
          "inventoryMode", "ReserveOnOrder"
        )))
        .execute();

    // Example "Create Cart": END!!!

    Map<String, Object> cartJson = gson.fromJson(cart.get().getResponseBody(), Map.class);
    String cartId = (String) cartJson.get("id");
    Integer cartVersion = ((Double) cartJson.get("version")).intValue();

    // Example "Set Shipping Address": START!!!

    cart =
      asyncHttpClient
        .preparePost("https://api.sphere.io/my-project-key/carts/" + cartId)
        .addHeader("Authorization", "Bearer " + token.get("access_token"))
        .setBody(gson.toJson(ImmutableMap.of(
          "version", cartVersion,
          "actions", ImmutableList.of(ImmutableMap.of(
            "action", "setShippingAddress",
            "address", ImmutableMap.of(
              "country", "DE",
              "city", "Berlin",
              "streetName", "Friedrichstraße",
              "streetNumber", "1"
            )
          ))
        )))
        .execute();

    // Example "Set Shipping Address": END!!!

    cartJson = gson.fromJson(cart.get().getResponseBody(), Map.class);
    cartVersion = ((Double) cartJson.get("version")).intValue();

    // Example "Add Line Item to Cart": START!!!

    String productId = "c8c6f130-9bca-4263-b6b6-abcd4cdde1b9";

    cart =
      asyncHttpClient
        .preparePost("https://api.sphere.io/my-project-key/carts/" + cartId)
        .addHeader("Authorization", "Bearer " + token.get("access_token"))
        .setBody(gson.toJson(ImmutableMap.of(
          "version", cartVersion,
          "actions", ImmutableList.of(ImmutableMap.of(
            "action", "addLineItem",
            "productId", productId,
            "variantId", 1,
            "quantity", 15
          ))
        )))
        .execute();

    // Example "Add Line Item to Cart": END!!!

    cartJson = gson.fromJson(cart.get().getResponseBody(), Map.class);
    cartVersion = ((Double) cartJson.get("version")).intValue();

    // Example "Create Order from Cart": START!!!

    cart =
      asyncHttpClient
        .preparePost("https://api.sphere.io/my-project-key/orders")
        .addHeader("Authorization", "Bearer " + token.get("access_token"))
        .setBody(gson.toJson(ImmutableMap.of(
          "id", cartId,
          "version", cartVersion,
          "paymentState", "Paid"
        )))
        .execute();

    // Example "Create Order from Cart": END!!!

    System.out.println(cart.get().getResponseBody());
  }
}
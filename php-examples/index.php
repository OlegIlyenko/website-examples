<?php
define("API_URL", "https://api.sphere.io/my-project-key");
define("CLIENT_ID", "my-client-id");
define("CLIENT_SECRET", "my-client-secret");
define("PERMISSIONS", "manage_project:my-project-key");

function curl(array $options = array()) { 
    $ch = curl_init(); 
    $defaults = array( 
        CURLOPT_RETURNTRANSFER => 1, 
        CURLOPT_FRESH_CONNECT => 1, 
        CURLOPT_TIMEOUT => 4, 
    );
    curl_setopt_array($ch, ($options + $defaults)); 
    if (!$result = curl_exec($ch)) {
        trigger_error(curl_error($ch)); 
    } 
    curl_close($ch); 
    return json_decode($result); 
} 

$data = http_build_query(array(
    "grant_type" => "client_credentials",
    "scope" => PERMISSIONS
));
$options = array( 
    CURLOPT_URL => "https://auth.sphere.io/oauth/token",
    CURLOPT_POST => 1, 
    CURLOPT_POSTFIELDS => $data,
    CURLOPT_HTTPAUTH => CURLAUTH_BASIC,
    CURLOPT_USERPWD => sprintf("%s:%s", CLIENT_ID, CLIENT_SECRET)
);
$auth = curl($options);
printf("Access token: $auth->access_token<br/>");

// NOTE: We probably can skip the variable part when we copy/paste it to the website (this one: `Future<Response> snowboards = `)
// Example for Almost everything section: START!!!!

$data = http_build_query(array(
    "lang"      => "en",
    "text"      => "snowboard",
    "filter"    => "variants.price.centAmount:range(10000 to 30000)"
));
$options = array( 
    CURLOPT_URL => API_URL . "/product-projections/search?$data",
    CURLOPT_HTTPHEADER => array("Authorization: Bearer $auth->access_token")
);
$search = curl($options);

// Example for Almost everything section: END!!!!

$product = $search->results[0];
printf("Product ID: $product->id<br/>");

// Example "Create Cart": START!!!

$data = json_encode(array(
    "currency"      => "EUR",
    "country"       => "DE",
    "inventoryMode" => "ReserveOnOrder"
));
$options = array( 
    CURLOPT_URL => API_URL . "/carts",
    CURLOPT_POST => 1, 
    CURLOPT_POSTFIELDS => $data,
    CURLOPT_HTTPHEADER => array("Authorization: Bearer $auth->access_token")
);
$cart = curl($options);

// Example "Create Cart": END!!!

printf("Cart ID: $cart->id, version: $cart->version, price: {$cart->totalPrice->centAmount}<br/>");

// Example "Set Shipping Address": START!!!

$data = json_encode(array(        
    "version" => $cart->version,
    "actions" => array(array(
        "action"    => "setShippingAddress",
        "address"   => array(
            "country"       => "DE",
            "city"          => "Berlin",
            "streetName"    => "Friedrichstraße",
            "streetNumber"  => "1"
        )
    )),            
));
$options = array( 
    CURLOPT_URL => API_URL . "/carts/" . $cart->id,
    CURLOPT_POST => 1, 
    CURLOPT_POSTFIELDS => $data,
    CURLOPT_HTTPHEADER => array("Authorization: Bearer $auth->access_token")
);
$cart = curl($options);

// Example "Set Shipping Address": END!!!

printf("Cart ID: $cart->id, version: $cart->version, price: {$cart->totalPrice->centAmount}<br/>");

// Example "Add Line Item to Cart": START!!!

$data = json_encode(array(        
    "version" => $cart->version,
    "actions" => array(array(
        "action"    => "addLineItem",
        "productId" => $product->id,
        "variantId" => $product->masterVariant->id,
        "quantity"  => 2
    )),            
));
$options = array( 
    CURLOPT_URL => API_URL . "/carts/" . $cart->id,
    CURLOPT_POST => 1, 
    CURLOPT_POSTFIELDS => $data,
    CURLOPT_HTTPHEADER => array("Authorization: Bearer $auth->access_token")
);
$cart = curl($options);

// Example "Add Line Item to Cart": END!!!

printf("Cart ID: $cart->id, version: $cart->version, price: {$cart->totalPrice->centAmount}<br/>");

// Example "Create Order from Cart": START!!!

$data = json_encode(array(        
    "id"            => $cart->id,
    "version"       => $cart->version,
    "paymentState"  => "Paid"
));

$options = array( 
    CURLOPT_URL => API_URL . "/orders",
    CURLOPT_POST => 1, 
    CURLOPT_POSTFIELDS => $data,
    CURLOPT_HTTPHEADER => array("Authorization: Bearer $auth->access_token")
);
$order = curl($options);

// Example "Create Order from Cart": END!!!

printf("Order ID: $order->id, state: $order->orderState, payment: $order->paymentState<br/>");
?>

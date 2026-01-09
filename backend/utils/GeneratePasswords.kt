import at.favre.lib.crypto.bcrypt.BCrypt

fun main() {
    val cost = 12
    
    val factory123Hash = BCrypt.withDefaults().hashToString(cost, "factory123".toCharArray())
    val retailer123Hash = BCrypt.withDefaults().hashToString(cost, "retailer123".toCharArray())
    
    println("Factory Owner Password Hash (cost=$cost):")
    println(factory123Hash)
    println()
    println("Retailer Password Hash (cost=$cost):")
    println(retailer123Hash)
    println()
    println("SQL Update Commands:")
    println("-- Factory Owner")
    println("UPDATE app_user SET password_hash = '$factory123Hash' WHERE email = 'factory@ganeshkulfi.com';")
    println()
    println("-- Retailer")
    println("UPDATE app_user SET password_hash = '$retailer123Hash' WHERE email = 'retailer@ganeshkulfi.com';")
}

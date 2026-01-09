import at.favre.lib.crypto.bcrypt.BCrypt

fun main() {
    val password = "Admin1234"
    val hash = "\$2a\$12\$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVKK6RZZ3i"
    
    println("Testing BCrypt verification:")
    println("Password: $password")
    println("Hash: $hash")
    
    val result = BCrypt.verifyer().verify(password.toCharArray(), hash)
    println("Verified: ${result.verified}")
    
    // Also test hashing the password
    val newHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
    println("\nNew hash: $newHash")
    val newVerify = BCrypt.verifyer().verify(password.toCharArray(), newHash)
    println("New verify: ${newVerify.verified}")
}

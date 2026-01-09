import at.favre.lib.crypto.bcrypt.BCrypt

fun main() {
    val password = "Admin@123"
    val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
    println("Password: $password")
    println("BCrypt hash: $hash")
}

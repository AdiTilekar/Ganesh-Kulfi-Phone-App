@file:DependsOn("at.favre.lib:bcrypt:0.10.2")

import at.favre.lib.crypto.bcrypt.BCrypt

val password = "Admin1234"
val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
println("Password: $password")
println("BCrypt Hash: $hash")

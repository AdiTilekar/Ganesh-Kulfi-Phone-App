import at.favre.lib.crypto.bcrypt.BCrypt;

public class GenHash {
    public static void main(String[] args) {
        String password = "Admin@123";
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash (cost 12):");
        System.out.println(hash);
        
        // Test verification
        boolean matches = BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
        System.out.println("Verification test: " + (matches ? "✅ PASS" : "❌ FAIL"));
    }
}

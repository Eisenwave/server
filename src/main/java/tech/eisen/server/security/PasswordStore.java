package tech.eisen.server.security;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PasswordStore {
    
    private final Map<String, char[]> userMap = new HashMap<>();
    
    public void setPassword(String user, char[] password) {
        char[] hash = SecureHashes.hashPasswordPBDKDF2(password);
        userMap.put(user, hash);
    }
    
    public void setHash(String user, char[] hash) {
        userMap.put(user, hash);
    }
    
    public boolean isRegistered(String user) {
        return userMap.containsKey(user);
    }
    
    /**
     * Returns the total amount of user/password pairs.
     *
     * @return the total amount of user/password pairs
     */
    public int size() {
        return userMap.size();
    }
    
    /**
     * Matches the combination of user name and password with the corresponding hash in the store.
     *
     * @param user the user
     * @param password the password
     * @return whether the password's hash matches the store hash
     */
    public boolean matchPassword(@NotNull String user, @NotNull char[] password) {
        char[] hash = userMap.get(user);
        if (hash == null)
            throw new IllegalArgumentException("user is not registered");
        
        return SecureHashes.validatePasswordPBDKDF2(password, hash);
    }
    
}

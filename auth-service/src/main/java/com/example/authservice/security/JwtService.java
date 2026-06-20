package com.example.authservice.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.key-id}")
    private String keyId;

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JwtService(
            @Value("classpath:keys/private_key.pem") Resource privatePem,
            @Value("classpath:keys/public_key.pem") Resource publicPem
    ) throws Exception {
        this.privateKey = loadPrivateKey(privatePem);
        this.publicKey = loadPublicKey(publicPem);
    }

    public String generateToken(UserDetails userDetails, Long userId) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim("username", userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities().stream().map(Object::toString).toList())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(expirationMs)))
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(keyId)
                .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new RSASSASigner(privateKey));
        return jwt.serialize();
    }

    public long getExpirationMs() { return expirationMs; }
    public RSAPublicKey getPublicKey() { return publicKey; }
    public String getKeyId() { return keyId; }

    // PEM files come as plain text with header/footer lines - strip those, then base64-decode
    private RSAPrivateKey loadPrivateKey(Resource pem) throws Exception {
        String key = new String(pem.getInputStream().readAllBytes())
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKey(Resource pem) throws Exception {
        String key = new String(pem.getInputStream().readAllBytes())
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}

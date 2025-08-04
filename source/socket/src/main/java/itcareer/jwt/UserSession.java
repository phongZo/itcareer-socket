package itcareer.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import itcareer.utils.SocketService;
import lombok.Data;
import org.apache.logging.log4j.Logger;

@Data
public class UserSession {
    private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger(UserSession.class);
    private Long id;
    private int kind;


    public static UserSession fromToken(String token){
        String publicKey = SocketService.getInstance().getStringResource("server.public.key");
        try {
            //System.out.println("==========> JWT secrect key: "+secretKey);
            Algorithm algorithm = Algorithm.HMAC256(publicKey);
            JWTVerifier verifier = JWT.require(algorithm)
                    .acceptLeeway(1) //1 sec for nbf and iat
                    .acceptExpiresAt(5) //5 secs for exp
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            Long userId = decodedJWT.getClaim("user_id").asLong();
            Integer userKind = decodedJWT.getClaim("user_kind").asInt();
            if(userId != null && userKind != null){
                UserSession userSession = new UserSession();
                userSession.setId(userId);
                userSession.setKind(userKind);
                return userSession;
            }

            return null;

        } catch (Exception e) {
            LOG.info("RSA key: {}", publicKey);
            LOG.info("verifierJWT>>{}", e.getMessage());
            return null;
        }
    }
}

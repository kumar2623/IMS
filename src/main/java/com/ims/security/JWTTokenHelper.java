package com.ims.security;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JWTTokenHelper {

	@Autowired
	private BlockListCacheService blockListCacheService;

//	@Value("${jwt.auth.app}")
	private String appName="Spring-Security-App";

//	@Value("${jwt.auth.secret_key}")
	private String secretKey="testkey#secret@12334";

//	@Value("${jwt.auth.expires_in}")
	private int expiresIn=3600;

	private SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

	private Claims getAllClaimsFromToken(String token) {
		Claims claims;
		try {
			claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
		} catch (Exception e) {
			claims = null;
		}
		return claims;
	}

	public String getUsernameFromToken(String token) {
		String username;
		try {
			final Claims claims = this.getAllClaimsFromToken(token);
			username = claims.getSubject();
		} catch (Exception e) {
			username = null;
		}
		return username;
	}

	public String generateToken(String username) throws InvalidKeySpecException, NoSuchAlgorithmException 
	{
//		System.err.println("------------- iiiiiiiiiii ----------------");
		if (blockListCacheService.isUserBlocked(username)) {
			blockListCacheService.evictUserFromBlockList(username);
			blockListCacheService.addUserToBlockList(username, "");
		}
		return Jwts.builder().setIssuer(appName).setSubject(username).setIssuedAt(new Date())
				.setExpiration(generateExpirationDate()).signWith(SIGNATURE_ALGORITHM, secretKey).compact();
	}

	private Date generateExpirationDate() {
		return new Date(new Date().getTime() + expiresIn * 1000);
	}

	public Boolean validateToken(String token, UserDetails userDetails) {
//		System.err.println("------------- vvvvvvvvvvvv ----------------");

		final String username = getUsernameFromToken(token);
//		System.out.println(blockListCacheService.isUserBlocked(username));
		if(blockListCacheService.isUserBlocked(username)) {
			return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));

		}
		return false;
		
		
	}

	public boolean isTokenExpired(String token) {
		Date expireDate = getExpirationDate(token);
		return expireDate.before(new Date());
	}

	private Date getExpirationDate(String token) {
		Date expireDate;
		try {
			final Claims claims = this.getAllClaimsFromToken(token);
			expireDate = claims.getExpiration();
		} catch (Exception e) {
			expireDate = null;
		}
		return expireDate;
	}

	public Date getIssuedAtDateFromToken(String token) {
		Date issueAt;
		try {
			final Claims claims = this.getAllClaimsFromToken(token);
			issueAt = claims.getIssuedAt();
		} catch (Exception e) {
			issueAt = null;
		}
		return issueAt;
	}

	public String getToken(HttpServletRequest request) {

		String authHeader = getAuthHeaderFromHeader(request);
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}

		return null;
	}

	public String getAuthHeaderFromHeader(HttpServletRequest request) {
		return request.getHeader("Authorization");
	}
}

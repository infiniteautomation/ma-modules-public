/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infiniteautomation.mango.rest.v2.exception.NotFoundRestException;
import com.infiniteautomation.mango.rest.v2.model.jwt.HeaderClaimsModel;
import com.infiniteautomation.mango.rest.v2.model.jwt.TokenModel;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.MangoRestController;
import com.serotonin.m2m2.web.mvc.spring.components.TokenAuthenticationService;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

/**
 * JSON Web Token REST endpoint.
 * 
 * WARNING! This REST controller is PUBLIC by default. Add @PreAuthorize annotations to restrict individual end-points.
 *
 * @author Jared Wiltshire
 */
@Api(value = "Authentication tokens", description = "Creates and verifies JWT (JSON web token) authentication tokens")
@RestController
@RequestMapping("/v2/auth-tokens")
public class AuthenticationTokenRestController extends MangoRestController {

    private static final int DEFAULT_EXPIRY = 5 * 60 * 1000; // 5 minutes
    
    private final TokenAuthenticationService tokenAuthService;
    private final MangoSessionRegistry sessionRegistry;

    @Autowired
    public AuthenticationTokenRestController(TokenAuthenticationService jwtService, MangoSessionRegistry sessionRegistry) {
        this.tokenAuthService = jwtService;
        this.sessionRegistry = sessionRegistry;
    }

    @ApiOperation(value = "Create token", notes = "Creates a token for the current user")
    @RequestMapping(path="/create", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated() and isPasswordAuthenticated()")
    public ResponseEntity<TokenModel> createToken(
            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE_TIME)
            Date expiry,
            
            @AuthenticationPrincipal User user) {

        if (expiry == null) {
            expiry = new Date(System.currentTimeMillis() + DEFAULT_EXPIRY);
        }

        String token = tokenAuthService.generateToken(user, expiry);
        return new ResponseEntity<>(new TokenModel(token), HttpStatus.CREATED);
    }
    
    @ApiOperation(value = "Revoke all tokens", notes = "Revokes all tokens for the current user")
    @RequestMapping(path="/revoke", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated() and isPasswordAuthenticated()")
    public ResponseEntity<Void> revokeTokens(
            @AuthenticationPrincipal User user,
            
            HttpServletRequest request) {
        
        tokenAuthService.revokeTokens(user);
        sessionRegistry.userUpdated(request, user);
        
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @ApiOperation(value = "Create token for user", notes = "Creates a token for a given user")
    @RequestMapping(path="/create/{username}", method = RequestMethod.POST)
    @PreAuthorize("isAdmin() and isPasswordAuthenticated()")
    public ResponseEntity<TokenModel> createTokenForUser(
            @PathVariable String username,
            
            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE_TIME)
            Date expiry) {

        if (expiry == null) {
            expiry = new Date(System.currentTimeMillis() + DEFAULT_EXPIRY);
        }

        User user = UserDao.instance.getUser(username);
        if (user == null) {
            throw new NotFoundRestException();
        }
        
        String token = tokenAuthService.generateToken(user, expiry);
        return new ResponseEntity<>(new TokenModel(token), HttpStatus.CREATED);
    }
    
    @ApiOperation(value = "Revoke all tokens for user", notes = "Revokes all tokens for a given user")
    @RequestMapping(path="/revoke/{username}", method = RequestMethod.POST)
    @PreAuthorize("isAdmin() and isPasswordAuthenticated()")
    public ResponseEntity<Void> createTokenForUser(
            @PathVariable String username,
            
            HttpServletRequest request) {

        User user = UserDao.instance.getUser(username);
        if (user == null) {
            throw new NotFoundRestException();
        }
        
        tokenAuthService.revokeTokens(user);
        sessionRegistry.userUpdated(request, user);
        
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @ApiOperation(value = "Gets the public key for verifying authentication tokens")
    @RequestMapping(path="/public-key", method = RequestMethod.GET)
    public String getPublicKey() {
        return this.tokenAuthService.getPublicKey();
    }

    @ApiOperation(value = "Verify the sigature and parse an authentication token", notes="Does NOT verify the claims")
    @RequestMapping(path="/verify", method = RequestMethod.GET)
    public HeaderClaimsModel verifyToken(
            @ApiParam(value = "The token to parse", required = true, allowMultiple = false)
            @RequestParam(required=true) String token) {
        
        Jws<Claims> jwsToken = this.tokenAuthService.parse(token);
        return new HeaderClaimsModel(jwsToken);
    }
    
    @ApiOperation(value = "Resets the public and private keys", notes = "Will invalidate all authentication tokens")
    @RequestMapping(path="/reset-keys", method = RequestMethod.POST)
    @PreAuthorize("isAdmin() and isPasswordAuthenticated()")
    public ResponseEntity<Void> resetKeys() {
        tokenAuthService.resetKeys();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

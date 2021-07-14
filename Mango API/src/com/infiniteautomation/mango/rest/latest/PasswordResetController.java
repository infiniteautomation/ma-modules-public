/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.rest.latest;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infiniteautomation.mango.jwt.JwtSignerVerifier;
import com.infiniteautomation.mango.rest.latest.exception.BadRequestException;
import com.infiniteautomation.mango.rest.latest.model.jwt.HeaderClaimsModel;
import com.infiniteautomation.mango.rest.latest.model.user.UserModel;
import com.infiniteautomation.mango.spring.components.PasswordResetService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.Validatable;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;
import com.serotonin.m2m2.web.mvc.spring.security.permissions.AnonymousAccess;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Password reset REST endpoints
 *
 * @author Jared Wiltshire
 */
@Api(value = "Password reset", description = "Endpoints for resetting user passwords")
@RestController
@RequestMapping("/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final MangoSessionRegistry sessionRegistry;

    @Autowired
    public PasswordResetController(PasswordResetService passwordResetService, MangoSessionRegistry sessionRegistry) {
        this.passwordResetService = passwordResetService;
        this.sessionRegistry = sessionRegistry;
    }

    @ApiOperation(value = "Sends the user an email containing a password reset link")
    @RequestMapping(method = RequestMethod.POST, value = "/send-email")
    @AnonymousAccess
    public ResponseEntity<Void> sendEmail(
            @RequestBody
            SendEmailRequestBody body) {

        body.ensureValid();
        passwordResetService.sendEmail(body.getUsername(), body.getEmail());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Resets the user's password if the token is correct")
    @RequestMapping(method = RequestMethod.POST, value = "/reset")
    @AnonymousAccess
    public ResponseEntity<Void> reset(
            @RequestBody PasswordResetRequestBody body) {

        body.ensureValid();
        try {
            passwordResetService.resetPassword(body.getToken(), body.getNewPassword());
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | IllegalArgumentException | SignatureException | MissingClaimException | IncorrectClaimException e) {
            throw new BadRequestException(new TranslatableMessage("rest.error.invalidPasswordResetToken"), e);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Gets the public key for verifying password reset tokens")
    @RequestMapping(path="/public-key", method = RequestMethod.GET)
    @AnonymousAccess
    public String getPublicKey() {
        return this.passwordResetService.getPublicKey();
    }

    @ApiOperation(value = "Verify the signature and parse a password reset token", notes="Does NOT verify the claims")
    @RequestMapping(path="/verify", method = RequestMethod.GET)
    @AnonymousAccess
    public HeaderClaimsModel verifyToken(
            @ApiParam(value = "The token to parse", required = true)
            @RequestParam String token) {

        try {
            return new HeaderClaimsModel(this.passwordResetService.parse(token));
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | IllegalArgumentException | SignatureException | MissingClaimException | IncorrectClaimException e) {
            throw new BadRequestException(new TranslatableMessage("rest.error.invalidPasswordResetToken"), e);
        }
    }

    @ApiOperation(value = "Resets the public and private keys", notes = "Will invalidate all password reset tokens")
    @RequestMapping(path="/reset-keys", method = RequestMethod.POST)
    @PreAuthorize("isAdmin() and isPasswordAuthenticated()")
    public ResponseEntity<Void> resetKeys() {
        passwordResetService.resetKeys();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Creates a password reset token and link for the given user")
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    @PreAuthorize("isAdmin() and isPasswordAuthenticated()")
    public CreateTokenResponse createTokenForUser(
            @RequestBody
            CreateTokenRequest requestBody) throws UnknownHostException {

        requestBody.ensureValid();
        String username = requestBody.getUsername();
        boolean lockPassword = requestBody.isLockPassword();
        boolean sendEmail = requestBody.isSendEmail();
        Date expiry = requestBody.getExpiry();

        CreateTokenResponse response = new CreateTokenResponse();
        String token = passwordResetService.generateToken(username, expiry, lockPassword, sendEmail);
        response.setToken(token);
        response.setFullUrl(passwordResetService.generateResetUrl(token));
        response.setRelativeUrl(passwordResetService.generateRelativeResetUrl(token));

        return response;
    }


    @ApiOperation(value = "Change admin password and set system locale, system timezone", notes = "Superadmin permission required")
    @RequestMapping(method = RequestMethod.POST, value="/system-setup")
    @PreAuthorize("isPasswordAuthenticated()")
    public ResponseEntity<UserModel> systemSetup(
            HttpServletRequest request,
            @RequestBody SystemSetupRequest body) {
        body.ensureValid();
        User update = passwordResetService.systemSetup(body.getPassword(), body.getSystemSettings());
        sessionRegistry.userUpdated(request, update);
        return new ResponseEntity<UserModel>(new UserModel(update), HttpStatus.OK);
    }

    public static class SystemSetupRequest implements Validatable {
        String password;
        Map<String, Object> systemSettings;

        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
        public Map<String, Object> getSystemSettings() {
            return systemSettings;
        }
        public void setSystemSettings(Map<String, Object> systemSettings) {
            this.systemSettings = systemSettings;
        }

        @Override
        public void validate(ProcessResult response) {
            SystemSettingsDao.instance.validate(systemSettings, response, Common.getUser());
        }

        @Override
        public String toString() {
            return "SystemSetupRequest [password=" + password + ", systemSettings=" + systemSettings + "]";
        }

    }

    public static class CreateTokenRequest implements Validatable {
        String username;
        boolean lockPassword = true;
        boolean sendEmail = false;
        Date expiry;

        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public boolean isLockPassword() {
            return lockPassword;
        }
        public void setLockPassword(boolean lockPassword) {
            this.lockPassword = lockPassword;
        }
        public boolean isSendEmail() {
            return sendEmail;
        }
        public void setSendEmail(boolean sendEmail) {
            this.sendEmail = sendEmail;
        }
        public Date getExpiry() {
            return expiry;
        }
        public void setExpiry(Date expiry) {
            this.expiry = expiry;
        }

        @Override
        public void validate(ProcessResult response) {
            if (username == null || username.isEmpty()) {
                response.addContextualMessage("username", "validate.required");
            }
        }

        @Override
        public String toString() {
            return "CreateTokenRequest [username=" + username + ", lockPassword=" + lockPassword
                    + ", sendEmail=" + sendEmail + ", expiry=" + expiry + "]";
        }

    }

    public static class CreateTokenResponse {
        private String token;
        private URI fullUrl;
        private URI relativeUrl;

        public String getToken() {
            return token;
        }
        public void setToken(String token) {
            this.token = token;
        }
        public URI getFullUrl() {
            return fullUrl;
        }
        public void setFullUrl(URI fullUrl) {
            this.fullUrl = fullUrl;
        }
        public URI getRelativeUrl() {
            return relativeUrl;
        }
        public void setRelativeUrl(URI relativeUrl) {
            this.relativeUrl = relativeUrl;
        }

        @Override
        public String toString() {
            return "CreateTokenResponse [token=" + JwtSignerVerifier.printToken(token) + ", fullUrl=" + fullUrl + ", relativeUrl="
                    + relativeUrl + "]";
        }
    }

    public static class SendEmailRequestBody implements Validatable {
        private String username;
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        public void validate(ProcessResult response) {
            if (email == null || email.isEmpty()) {
                response.addContextualMessage("email", "validate.required");
            }
            if (username == null || username.isEmpty()) {
                response.addContextualMessage("username", "validate.required");
            }
        }
    }

    public static class PasswordResetRequestBody implements Validatable {
        private String token;
        private String newPassword;

        public String getToken() {
            return token;
        }
        public void setToken(String token) {
            this.token = token;
        }
        public String getNewPassword() {
            return newPassword;
        }
        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        @Override
        public void validate(ProcessResult response) {
            if (token == null || token.isEmpty()) {
                response.addContextualMessage("token", "validate.required");
            }
            if (newPassword == null || newPassword.isEmpty()) {
                response.addContextualMessage("newPassword", "validate.required");
            }
        }
    }
}
